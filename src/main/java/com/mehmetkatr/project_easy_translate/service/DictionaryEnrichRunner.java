package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.repository.DictionaryEntryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OFFLINE ENRICH islemcisi (Gemini, bir kerelik, ucretsiz):
 *   her kelimenin en yaygin 3 anlamini secip siralar (common_rank) ve
 *   ornegi olmayan anlama Ingilizce ornek uretir (example_ai). Ceviri YAPMAZ.
 *
 * Tetikleme:
 *   DICTIONARY_ENRICH=true                 # bos/false ise calismaz
 *   DICTIONARY_ENRICH_BATCH=10             # tek Gemini cagrisindaki kelime sayisi
 *   DICTIONARY_ENRICH_LIMIT=0              # 0=hepsi, yoksa ilk N (A1 once)
 *   DICTIONARY_ENRICH_DELAY_MS=4500        # cagrilar arasi bekleme (free ~15 RPM)
 *
 * Guvenli: zaten islenmis (ai_enriched) kelimeleri atlar (resumable), ilerleme loglar.
 */
@Component
@RequiredArgsConstructor
public class DictionaryEnrichRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DictionaryEnrichRunner.class);

    private final DictionaryEntryRepository entryRepository;
    private final DictionaryService dictionaryService;

    @Value("${talemind.dictionary.enrich:${DICTIONARY_ENRICH:false}}")
    private boolean enabled;

    @Value("${talemind.dictionary.enrich-batch:${DICTIONARY_ENRICH_BATCH:10}}")
    private int batchSize;

    @Value("${talemind.dictionary.enrich-limit:${DICTIONARY_ENRICH_LIMIT:0}}")
    private int limit;

    @Value("${talemind.dictionary.enrich-delay-ms:${DICTIONARY_ENRICH_DELAY_MS:4500}}")
    private long delayMs;

    @Override
    public void run(String... args) {
        if (!enabled) return;
        if (batchSize < 1) batchSize = 10;

        List<Long> ids = entryRepository.findAllIdsByCefr(); // A1 -> C2
        if (limit > 0 && limit < ids.size()) ids = ids.subList(0, limit);

        int total = ids.size();
        log.info("[sozluk-enrich] Baslatiliyor. kelime={} batch={} delayMs={}", total, batchSize, delayMs);
        long t0 = System.currentTimeMillis();
        int processed = 0, enriched = 0, batchNo = 0, geminiFailStreak = 0;

        for (int i = 0; i < total; i += batchSize) {
            List<Long> page = ids.subList(i, Math.min(i + batchSize, total));
            batchNo++;
            int res = 0;
            try {
                res = dictionaryService.enrichPage(page);
            } catch (Exception e) {
                log.warn("[sozluk-enrich] batch {} hatasi: {}", batchNo, e.getMessage());
            }
            if (res < 0) {
                geminiFailStreak++;
                if (geminiFailStreak >= 4) {
                    log.warn("[sozluk-enrich] Gemini'ye ust uste ulasilamadi (muhtemelen gunluk kota). " +
                            "DURDURULDU — ilerleme kaydedildi, sonra DICTIONARY_ENRICH=true ile kaldigin yerden devam et.");
                    break;
                }
            } else {
                geminiFailStreak = 0;
                enriched += res;
            }
            processed += page.size();
            log.info("[sozluk-enrich] ilerleme: {}/{}  (zenginlesen kelime: {})", processed, total, enriched);

            if (i + batchSize < total && delayMs > 0) {
                try { Thread.sleep(delayMs); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); break; }
            }
        }
        log.info("[sozluk-enrich] BITTI. islenen={} zenginlesen={} sure={}sn",
                processed, enriched, (System.currentTimeMillis() - t0) / 1000);
    }
}
