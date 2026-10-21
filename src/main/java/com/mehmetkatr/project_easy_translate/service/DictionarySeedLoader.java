package com.mehmetkatr.project_easy_translate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mehmetkatr.project_easy_translate.entity.DictionaryEntry;
import com.mehmetkatr.project_easy_translate.entity.DictionarySense;
import com.mehmetkatr.project_easy_translate.entity.DictionarySenseTr;
import com.mehmetkatr.project_easy_translate.repository.DictionaryEntryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * TaleMind Sozlugu yukleyicisi — kaikki ETL ciktisi (dictionary.jsonl) -> DB.
 *
 * Tetikleme (uygulamayi bir kez calistir):
 *   DICTIONARY_SEED_PATH=/.../out/dictionary.jsonl
 * Guvenlik:
 *   - Yol bos ise hic calismaz.
 *   - Tablo zaten doluysa atlar (DICTIONARY_SEED_FORCE=true ile zorlanir).
 *   - Parti parti (batch) yazar, ilerleme loglar.
 */
@Component
@RequiredArgsConstructor
public class DictionarySeedLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DictionarySeedLoader.class);
    private static final int BATCH = 500;

    private final DictionaryEntryRepository entryRepository;
    private final ObjectMapper objectMapper;

    @Value("${talemind.dictionary.seed-path:${DICTIONARY_SEED_PATH:}}")
    private String seedPath;

    @Value("${talemind.dictionary.seed-force:${DICTIONARY_SEED_FORCE:false}}")
    private boolean force;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (seedPath == null || seedPath.isBlank()) {
            return; // yukleme istenmedi
        }
        Path file = Path.of(seedPath.trim());
        if (!Files.isRegularFile(file)) {
            log.warn("[sozluk-yukle] Dosya bulunamadi: {}", file.toAbsolutePath());
            return;
        }
        long existing = entryRepository.count();
        if (existing > 0 && !force) {
            log.info("[sozluk-yukle] Tabloda {} kayit var, atlaniyor (zorlamak icin DICTIONARY_SEED_FORCE=true).", existing);
            return;
        }

        log.info("[sozluk-yukle] Baslatiliyor: {}", file.toAbsolutePath());
        long t0 = System.currentTimeMillis();
        int ok = 0, bad = 0, line = 0;
        List<DictionaryEntry> buffer = new ArrayList<>(BATCH);

        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String raw;
            while ((raw = br.readLine()) != null) {
                line++;
                String s = raw.trim();
                if (s.isEmpty()) continue;
                try {
                    buffer.add(toEntry(objectMapper.readTree(s)));
                    ok++;
                } catch (Exception e) {
                    bad++;
                    if (bad <= 5) log.warn("[sozluk-yukle] Satir {} atlandi: {}", line, e.getMessage());
                }
                if (buffer.size() >= BATCH) {
                    entryRepository.saveAll(buffer);
                    entryRepository.flush();
                    buffer.clear();
                    log.info("[sozluk-yukle] yuklenen: {}", ok);
                }
            }
            if (!buffer.isEmpty()) {
                entryRepository.saveAll(buffer);
                entryRepository.flush();
            }
        }
        log.info("[sozluk-yukle] BITTI. kayit={} hatali={} sure={}sn",
                ok, bad, (System.currentTimeMillis() - t0) / 1000);
    }

    private DictionaryEntry toEntry(JsonNode n) {
        DictionaryEntry entry = DictionaryEntry.builder()
                .word(text(n, "word"))
                .languageCode("en")
                .cefr(text(n, "cefr"))
                .topic(text(n, "topic"))
                .source(n.path("source").asText("CEFR"))
                .idiom(n.path("idiom").asBoolean(false))
                .aiEnriched(true)                 // saf Gemini kart: hazir, runtime enrich gerekmez
                .formsJson(jsonOrEmpty(n.get("forms")))
                .build();

        int order = 0;
        JsonNode senses = n.get("senses");
        if (senses != null && senses.isArray()) {
            for (JsonNode sn : senses) {
                // tanim: yeni format def_en, geri uyum icin definition
                String defEn = text(sn, "def_en");
                if (defEn == null) defEn = text(sn, "definition");

                // ornekleri EN/TR'ye ayir ([{en,tr}] veya eski duz string)
                List<String> exEn = new ArrayList<>();
                List<String> exTr = new ArrayList<>();
                JsonNode exs = sn.get("examples");
                if (exs != null && exs.isArray()) {
                    for (JsonNode ex : exs) {
                        if (ex.isObject()) {
                            String en = text(ex, "en");
                            if (en != null && !en.isBlank()) {
                                exEn.add(en);
                                exTr.add(ex.path("tr").asText(""));
                            }
                        } else if (ex.isTextual() && !ex.asText().isBlank()) {
                            exEn.add(ex.asText());
                        }
                    }
                }

                DictionarySense sense = DictionarySense.builder()
                        .entry(entry)
                        .senseOrder(order)
                        .commonRank(order)
                        .exampleAi(true)
                        .pos(text(sn, "pos"))
                        .idiom(sn.path("_idiom").asBoolean(false))
                        .definition(defEn)
                        .examplesJson(toJsonArray(exEn))
                        .synonymsJson(jsonOrEmpty(sn.get("synonyms")))
                        .build();

                // TR cevirisi (hazir) -> dictionary_sense_tr
                String defTr = text(sn, "def_tr");
                if ((defTr != null && !defTr.isBlank()) || !exTr.isEmpty()) {
                    DictionarySenseTr tr = DictionarySenseTr.builder()
                            .sense(sense)
                            .targetLang("tr")
                            .definitionTr(defTr)
                            .examplesTrJson(toJsonArray(exTr))
                            .engine("GEMINI")
                            .displayRank(order)
                            .build();
                    sense.getTranslations().add(tr);
                }

                entry.getSenses().add(sense);
                order++;
            }
        }
        return entry;
    }

    private String toJsonArray(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    /** Diziyi JSON string olarak dondur; yoksa "[]". */
    private String jsonOrEmpty(JsonNode arr) {
        if (arr == null || arr.isNull()) return "[]";
        return arr.toString();
    }
}
