package com.mehmetkatr.project_easy_translate.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sozluk enrich'ini ARKA PLANDA calistirir (kullanici beklemez).
 * getCard, zenginlesmemis bir kelime icin {@link EnrichRequested} yayinlar;
 * bu dinleyici ayri thread'de Gemini enrich'i yapar (siralama + eksik ornek).
 * Ayni kelime icin es zamanli tekrarı engellemek icin in-progress kaydi tutulur.
 */
@Component
@RequiredArgsConstructor
public class DictionaryEnrichListener {

    private static final Logger log = LoggerFactory.getLogger(DictionaryEnrichListener.class);

    private final DictionaryService dictionaryService;
    private final Set<Long> inProgress = ConcurrentHashMap.newKeySet();

    /** getCard'in yayinladigi olay: su entry'yi arka planda zenginlestir. */
    public record EnrichRequested(Long entryId) {}

    @Async
    @EventListener
    public void onEnrichRequested(EnrichRequested event) {
        Long id = event.entryId();
        if (id == null || !inProgress.add(id)) return; // zaten isleniyor
        try {
            int done = dictionaryService.enrichPage(List.of(id));
            log.debug("[sozluk-enrich-async] entry {} islendi (done={})", id, done);
        } catch (Exception e) {
            log.info("[sozluk-enrich-async] entry {} hatasi: {}", id, e.getMessage());
        } finally {
            inProgress.remove(id);
        }
    }
}
