package com.mehmetkatr.project_easy_translate.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mehmetkatr.project_easy_translate.dto.response.DictionaryCardResponse;
import com.mehmetkatr.project_easy_translate.entity.DictionaryEntry;
import com.mehmetkatr.project_easy_translate.entity.DictionarySense;
import com.mehmetkatr.project_easy_translate.entity.DictionarySenseTr;
import com.mehmetkatr.project_easy_translate.entity.TranslationCache;
import com.mehmetkatr.project_easy_translate.repository.DictionaryEntryRepository;
import com.mehmetkatr.project_easy_translate.repository.DictionarySenseTrRepository;
import com.mehmetkatr.project_easy_translate.repository.TranslationCacheRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * TaleMind Sozlugu runtime + enrich servisi.
 *
 * Runtime (kart):
 *  - Anlamlar DB'den; gosterilecek 3 anlam {@code common_rank}'e gore (Gemini enrich siralamasi),
 *    enrich yoksa Wiktionary sirasiyla ilk 3.
 *  - Turkce ceviri lazy: Microsoft Translator (key varsa) / yoksa MyMemory; dictionary_sense_tr'ye cache.
 *  - Ornekler: Wiktionary'den veya Gemini enrich ile uretilmis (example_ai).
 *
 * Enrich (offline, bir kez, Gemini):
 *  - Her kelimenin en yaygin 3 anlamini secip siralar (common_rank),
 *    ornegi olmayan anlama Ingilizce ornek uretir (example_ai). Ceviri YAPMAZ.
 */
@Service
@RequiredArgsConstructor
public class DictionaryService {

    private static final Logger log = LoggerFactory.getLogger(DictionaryService.class);
    private static final String SRC_DEFINITION = "Wiktionary (CC BY-SA)";
    private static final TypeReference<List<String>> STR_LIST = new TypeReference<>() {};
    private static final int MAX_EXAMPLES = 2;
    private static final int TOP_SENSES = 3;     // kartta gosterilecek anlam sayisi
    private static final int MAX_CANDIDATES = 12; // Gemini'ye sunulacak aday anlam ust siniri

    private final DictionaryEntryRepository entryRepository;
    private final DictionarySenseTrRepository senseTrRepository;
    private final TranslationCacheRepository translationCacheRepository;
    private final MicrosoftTranslatorClient microsoftTranslator;  // key varsa birincil
    private final MyMemoryTranslatorClient myMemoryTranslator;     // anahtarsiz varsayilan
    private final GeminiClient geminiClient;                       // sadece offline enrich
    private final ObjectMapper objectMapper;

    // ================================ RUNTIME (kart) ================================

    @Transactional(readOnly = true)
    public boolean exists(String word) {
        return word != null && !word.isBlank()
                && entryRepository.existsByWordIgnoreCaseAndLanguageCodeIgnoreCase(word.trim(), "en");
    }

    @Transactional
    public Optional<DictionaryCardResponse> getCard(String word, String rawTarget) {
        if (word == null || word.isBlank()) return Optional.empty();
        String target = (rawTarget == null || rawTarget.isBlank()) ? "tr" : rawTarget.trim().toLowerCase();

        Optional<DictionaryEntry> opt = entryRepository
                .findByWordIgnoreCaseAndLanguageCodeIgnoreCase(word.trim(), "en");
        if (opt.isEmpty()) return Optional.empty();
        DictionaryEntry entry = opt.get();

        // Lazy enrich: bu kelime daha once zenginlesmediyse, ILK acilista Gemini ile
        // (en yaygin 3 anlam sirasi + eksik ornek) yap ve kaydet. Basarisizsa (kota/503)
        // isaretlenmez, Wiktionary sirasiyla gosterilir; sonraki acilista tekrar denenir.
        if (!entry.isAiEnriched()) {
            try {
                enrichPage(java.util.List.of(entry.getId()));
            } catch (Exception e) {
                log.debug("lazy enrich hatasi: {}", e.getMessage());
            }
        }

        List<DictionarySense> chosen = chosenSenses(entry);
        ensureTranslations(chosen, target);

        String primary = primaryTranslation(entry.getWord(), target);

        boolean anyAiExample = false;
        List<DictionaryCardResponse.Sense> senses = new ArrayList<>();
        for (DictionarySense s : chosen) {
            if (s.isExampleAi()) anyAiExample = true;
            DictionarySenseTr tr = trFor(s, target);
            senses.add(new DictionaryCardResponse.Sense(
                    s.getPos(),
                    s.getDefinition(),
                    tr == null ? null : tr.getDefinitionTr(),
                    cleanExamples(s.getExamplesJson()),
                    tr == null ? List.of() : parseStringList(tr.getExamplesTrJson()),
                    parseStringList(s.getSynonymsJson())
            ));
        }

        String exampleSource = anyAiExample ? "AI (Gemini)" : "Wiktionary";
        return Optional.of(new DictionaryCardResponse(
                entry.getWord(), entry.getCefr(), entry.isIdiom(),
                target, primary, SRC_DEFINITION, engineName(), exampleSource,
                parseForms(entry.getFormsJson()), senses));
    }

    /** Kartta gosterilecek anlamlar: enrich varsa common_rank'e gore ilk 3, yoksa Wiktionary ilk 3. */
    private List<DictionarySense> chosenSenses(DictionaryEntry entry) {
        List<DictionarySense> ranked = entry.getSenses().stream()
                .filter(s -> s.getCommonRank() != null)
                .sorted(Comparator.comparingInt(DictionarySense::getCommonRank))
                .limit(TOP_SENSES)
                .toList();
        if (!ranked.isEmpty()) return ranked;
        return entry.getSenses().stream()
                .filter(s -> s.getDefinition() != null && !s.getDefinition().isBlank())
                .limit(TOP_SENSES)
                .toList();
    }

    /** Secili anlamlardan hedef dilde cevirisi olmayanlari TEK NMT cagrisiyla cevir + kaydet. */
    private void ensureTranslations(List<DictionarySense> chosen, String target) {
        List<DictionarySense> missing = chosen.stream()
                .filter(s -> s.getDefinition() != null && !s.getDefinition().isBlank())
                .filter(s -> trFor(s, target) == null)
                .toList();
        if (missing.isEmpty()) return;

        // Tek batch: her anlamin tanimi + ornekleri
        List<String> texts = new ArrayList<>();
        List<int[]> map = new ArrayList<>();
        List<List<String>> examplesPerSense = new ArrayList<>();
        for (int si = 0; si < missing.size(); si++) {
            texts.add(oneLine(missing.get(si).getDefinition()));
            map.add(new int[]{si, -1});
            List<String> exs = cleanExamples(missing.get(si).getExamplesJson());
            examplesPerSense.add(exs);
            for (int ei = 0; ei < exs.size(); ei++) {
                texts.add(oneLine(exs.get(ei)));
                map.add(new int[]{si, ei});
            }
        }

        List<String> tr = engineTranslate(texts, "en", target);
        if (tr.size() != texts.size()) return; // basarisiz -> cevirisiz birak
        String engine = engineName();

        String[] defTr = new String[missing.size()];
        List<List<String>> exTr = new ArrayList<>();
        for (int i = 0; i < missing.size(); i++) exTr.add(new ArrayList<>());
        for (int i = 0; i < map.size(); i++) {
            int si = map.get(i)[0], ei = map.get(i)[1];
            if (ei < 0) defTr[si] = tr.get(i);
            else exTr.get(si).add(tr.get(i));
        }

        for (int si = 0; si < missing.size(); si++) {
            if (defTr[si] == null || defTr[si].isBlank()) continue;
            DictionarySense s = missing.get(si);
            DictionarySenseTr row = DictionarySenseTr.builder()
                    .sense(s).targetLang(target)
                    .definitionTr(defTr[si].trim())
                    .examplesTrJson(toJson(exTr.get(si)))
                    .engine(engine).displayRank(si)
                    .build();
            s.getTranslations().add(row);
            senseTrRepository.save(row);
        }
    }

    /** Kelime geneli kisa anlam: translation_cache'ten; yoksa NMT ile cevir + cache. */
    private String primaryTranslation(String word, String target) {
        String key = word.toLowerCase();
        var cached = translationCacheRepository
                .findBySourceTextAndSourceLangAndTargetLang(key, "en", target);
        if (cached.isPresent()) return safeLower(cached.get().getTranslation());

        List<String> out = engineTranslate(List.of(word), "en", target);
        if (out.isEmpty() || out.get(0) == null || out.get(0).isBlank()) return "";
        String value = out.get(0).trim();
        try {
            translationCacheRepository.save(TranslationCache.builder()
                    .sourceText(key).sourceLang("en").targetLang(target)
                    .translation(value).engine(engineName()).build());
        } catch (Exception e) {
            log.debug("primary cache yazilamadi: {}", e.getMessage());
        }
        return safeLower(value);
    }

    /** NMT: Microsoft key varsa onu, yoksa (ya da bos donerse) MyMemory. */
    private List<String> engineTranslate(List<String> texts, String from, String to) {
        if (microsoftTranslator.isConfigured()) {
            List<String> r = microsoftTranslator.translate(texts, from, to);
            if (r.size() == texts.size()) return r;
        }
        return myMemoryTranslator.translate(texts, from, to);
    }

    private String engineName() {
        return microsoftTranslator.isConfigured() ? "Microsoft Translator" : "MyMemory";
    }

    // ============================= ENRICH (offline, Gemini) =============================

    /**
     * Bir sayfa kelimeyi TEK Gemini cagrisiyla zenginlestirir:
     *  - en yaygin 3 anlami secip siralar (common_rank),
     *  - ornegi olmayan anlama Ingilizce ornek uretir (example_ai).
     * Ceviri YAPMAZ. Islenen kelime sayisini dondurur.
     */
    @Transactional
    public int enrichPage(List<Long> entryIds) {
        List<DictionaryEntry> entries = entryRepository.findAllById(entryIds).stream()
                .filter(e -> !e.isAiEnriched())
                .toList();
        if (entries.isEmpty()) return 0;

        var candMap = new java.util.LinkedHashMap<String, List<DictionarySense>>();
        StringBuilder block = new StringBuilder();
        for (DictionaryEntry e : entries) {
            List<DictionarySense> cands = candidatesOf(e);
            if (cands.isEmpty()) continue;
            candMap.put(e.getWord(), cands);
            block.append("WORD: ").append(e.getWord()).append("\n")
                    .append(itemsBlock(cands)).append("\n\n");
        }
        if (candMap.isEmpty()) {
            entries.forEach(e -> e.setAiEnriched(true));
            entryRepository.saveAll(entries);
            return 0;
        }

        String prompt = """
                For each English word below, choose its %d MOST COMMONLY USED senses in modern everyday
                English, ordered from most to least common. For each chosen sense, provide ONE short,
                natural English example sentence: if a usable example is already listed for that sense,
                reuse it; otherwise WRITE a new simple sentence that clearly shows THAT sense.
                Return ONLY a compact JSON object (no markdown, no commentary) mapping each word to an
                array (most common first):
                {"<word>": [{"n": <candidate number>, "ex": "<english example>"}], ...}

                %s
                """.formatted(TOP_SENSES, block.toString().trim());

        String raw = callGeminiWithRetry(prompt);
        JsonNode obj = null;
        if (raw != null) {
            try { obj = objectMapper.readTree(stripObject(raw)); }
            catch (Exception e) { log.info("[sozluk-enrich] JSON parse hatasi: {}", e.getMessage()); }
        }
        if (obj == null) {
            return -1; // Gemini cagrisi basarisiz (kota/503) -> runner tekrarlari sayip durabilir
        }

        int done = 0;
        for (DictionaryEntry e : entries) {
            List<DictionarySense> cands = candMap.get(e.getWord());
            if (cands == null) {           // aday anlam yok -> yapacak is yok, isaretle gec
                e.setAiEnriched(true);
                continue;
            }
            JsonNode arr = (obj == null) ? null : obj.get(e.getWord());
            if (arr == null || !arr.isArray()) {
                continue;                   // Gemini bu kelime icin cevap vermedi -> ISARETLEME, sonra tekrar denenir
            }
            int rank = 0;
            for (JsonNode item : arr) {
                int n = item.path("n").asInt(-1);
                if (n < 1 || n > cands.size()) continue;
                DictionarySense s = cands.get(n - 1);
                if (s.getCommonRank() != null) continue; // ayni anlam tekrar gelirse atla
                s.setCommonRank(rank++);
                // ornek: mevcut temiz ornek yoksa Gemini'nin urettigini koy
                if (cleanExamples(s.getExamplesJson()).isEmpty()) {
                    String ex = item.path("ex").asText("").trim();
                    if (!ex.isEmpty()) {
                        s.setExamplesJson(toJson(List.of(ex)));
                        s.setExampleAi(true);
                    }
                }
                if (rank >= TOP_SENSES) break;
            }
            if (rank > 0) {                 // en az bir anlam siralandi -> basarili, isaretle
                done++;
                e.setAiEnriched(true);
            }
            // rank==0 (bos/gecersiz cevap) -> isaretleme, sonraki calistirmada tekrar denenir
        }
        entryRepository.saveAll(entries);
        return done;
    }

    private List<DictionarySense> candidatesOf(DictionaryEntry entry) {
        return entry.getSenses().stream()
                .filter(s -> s.getDefinition() != null && !s.getDefinition().isBlank())
                .limit(MAX_CANDIDATES)
                .toList();
    }

    private String itemsBlock(List<DictionarySense> candidates) {
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            DictionarySense s = candidates.get(i);
            items.append(i + 1).append(". (").append(s.getPos() == null ? "?" : s.getPos()).append(") ")
                    .append(oneLine(s.getDefinition()));
            List<String> ex = cleanExamples(s.getExamplesJson());
            if (!ex.isEmpty()) items.append("  | example: ").append(oneLine(ex.get(0)));
            items.append("\n");
        }
        return items.toString().trim();
    }

    private String callGeminiWithRetry(String prompt) {
        String raw = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                raw = geminiClient.generate(prompt);
                if (raw != null && !raw.isBlank()) break;
            } catch (Exception e) {
                log.info("[sozluk-enrich] Gemini denemesi {} hatasi: {}", attempt, e.getMessage());
            }
            if (attempt < 3) {
                try { Thread.sleep(800L * attempt); }
                catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }
        }
        return (raw == null || raw.isBlank()) ? null : raw;
    }

    // ================================ yardimcilar ================================

    private DictionarySenseTr trFor(DictionarySense s, String target) {
        return s.getTranslations().stream()
                .filter(t -> target.equalsIgnoreCase(t.getTargetLang()))
                .findFirst().orElse(null);
    }

    private String stripObject(String raw) {
        String s = raw.trim();
        int a = s.indexOf('{'), b = s.lastIndexOf('}');
        return (a >= 0 && b > a) ? s.substring(a, b + 1) : s;
    }

    private List<DictionaryCardResponse.Form> parseForms(String json) {
        List<DictionaryCardResponse.Form> forms = new ArrayList<>();
        if (json == null || json.isBlank()) return forms;
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.isArray()) {
                for (JsonNode f : node) {
                    List<String> tags = new ArrayList<>();
                    JsonNode tn = f.get("tags");
                    if (tn != null && tn.isArray()) tn.forEach(t -> tags.add(t.asText()));
                    forms.add(new DictionaryCardResponse.Form(f.path("form").asText(""), tags));
                }
            }
        } catch (Exception e) {
            log.debug("forms parse hatasi: {}", e.getMessage());
        }
        return forms;
    }

    /** Ornekleri parse et, arkaik/bozuk olanlari (uzun-s "ſ") ele, en fazla MAX_EXAMPLES. */
    private List<String> cleanExamples(String json) {
        List<String> raw = parseStringList(json);
        List<String> out = new ArrayList<>();
        for (String ex : raw) {
            if (ex == null) continue;
            String t = ex.trim();
            if (t.isEmpty()) continue;
            if (t.indexOf('ſ') >= 0 || t.indexOf('ʒ') >= 0) continue; // ſ, ʒ -> arkaik
            out.add(t);
            if (out.size() >= MAX_EXAMPLES) break;
        }
        return out;
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, STR_LIST); }
        catch (Exception e) { return List.of(); }
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        try { return objectMapper.writeValueAsString(list); }
        catch (Exception e) { return "[]"; }
    }

    private String oneLine(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }

    private String safeLower(String s) {
        return s == null ? "" : s.toLowerCase();
    }
}
