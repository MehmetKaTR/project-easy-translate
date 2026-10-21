package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.dto.response.StudyCategoriesResponse;
import com.mehmetkatr.project_easy_translate.dto.response.StudyItemResponse;
import com.mehmetkatr.project_easy_translate.dto.response.StudyStatsResponse;
import com.mehmetkatr.project_easy_translate.entity.DictionaryEntry;
import com.mehmetkatr.project_easy_translate.entity.DictionarySense;
import com.mehmetkatr.project_easy_translate.entity.DictionarySenseTr;
import com.mehmetkatr.project_easy_translate.entity.UserStudyProgress;
import com.mehmetkatr.project_easy_translate.repository.DictionaryEntryRepository;
import com.mehmetkatr.project_easy_translate.repository.UserStudyProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Kelime calisma (quiz) + SRS (SM-2).
 * Havuz UNIVERSAL (dictionary_entry); katsayi/ilerleme KULLANICIYA ozel (user_study_progress).
 */
@Service
@RequiredArgsConstructor
public class StudyService {

    private static final double EASE_MIN = 1.3;
    private static final double EASE_START = 2.5;

    private final UserStudyProgressRepository progressRepository;
    private final DictionaryEntryRepository entryRepository;

    /** Siradaki quiz kelimeleri. category: "level:A1" | "topic:Food & Drink" | "source:ANKI" | null(hepsi). */
    @Transactional(readOnly = true)
    public List<StudyItemResponse> next(Long userId, int count, List<Long> exclude, String category) {
        int want = Math.min(Math.max(count, 1), 60);
        LocalDate today = LocalDate.now();
        List<Long> ex = (exclude == null || exclude.isEmpty()) ? null : exclude;

        String src = null, cefr = null, topic = null;
        if (category != null && category.contains(":")) {
            String type = category.substring(0, category.indexOf(':'));
            String val = category.substring(category.indexOf(':') + 1).trim();
            switch (type) {
                case "level" -> { cefr = val; src = "CEFR"; }
                case "topic" -> { topic = val; src = "CEFR"; }
                case "source" -> src = val;
                default -> { }
            }
        }
        final String fsrc = src, fcefr = cefr, ftopic = topic;

        // 1) due tekrarlar (fazlasini al, kategoriye gore eleyecegiz)
        List<UserStudyProgress> due = progressRepository.findDue(userId, today, ex, PageRequest.of(0, want * 2));
        List<Long> order = new ArrayList<>();
        Map<Long, Boolean> isNew = new LinkedHashMap<>();
        for (UserStudyProgress p : due) { order.add(p.getEntryId()); isNew.put(p.getEntryId(), false); }

        // 2) eksik kalirsa yeni kelime (kategori filtreli)
        int remaining = want - order.size();
        if (remaining > 0) {
            List<Long> skip = new ArrayList<>(order);
            if (ex != null) skip.addAll(ex);
            List<Long> newIds = entryRepository.findNewEntryIds(userId, skip.isEmpty() ? null : skip,
                    fsrc, fcefr, ftopic, PageRequest.of(0, remaining));
            for (Long id : newIds) { order.add(id); isNew.put(id, true); }
        }
        if (order.isEmpty()) return List.of();

        Map<Long, DictionaryEntry> byId = new LinkedHashMap<>();
        for (DictionaryEntry e : entryRepository.findAllById(order)) byId.put(e.getId(), e);

        List<StudyItemResponse> items = new ArrayList<>();
        for (Long id : order) {
            DictionaryEntry e = byId.get(id);
            if (e == null) continue;
            if (fsrc != null && !fsrc.equalsIgnoreCase(e.getSource())) continue;
            if (fcefr != null && !fcefr.equalsIgnoreCase(e.getCefr())) continue;
            if (ftopic != null && !ftopic.equalsIgnoreCase(e.getTopic())) continue;
            items.add(toItem(e, isNew.getOrDefault(id, true)));
            if (items.size() >= want) break;
        }
        return items;
    }

    /** Calisma kategorileri (seviye + konu + Anki) sayilariyla. */
    @Transactional(readOnly = true)
    public StudyCategoriesResponse categories() {
        List<StudyCategoriesResponse.Cat> levels = new ArrayList<>();
        for (Object[] r : entryRepository.countByCefr("CEFR")) {
            String lvl = (String) r[0];
            levels.add(new StudyCategoriesResponse.Cat("level:" + lvl, lvl, ((Number) r[1]).longValue()));
        }
        List<StudyCategoriesResponse.Cat> topics = new ArrayList<>();
        for (Object[] r : entryRepository.countByTopic("CEFR")) {
            String t = (String) r[0];
            topics.add(new StudyCategoriesResponse.Cat("topic:" + t, t, ((Number) r[1]).longValue()));
        }
        List<StudyCategoriesResponse.Cat> special = new ArrayList<>();
        long anki = entryRepository.countBySource("ANKI");
        if (anki > 0) special.add(new StudyCategoriesResponse.Cat("source:ANKI", "Anki (tümü)", anki));
        return new StudyCategoriesResponse(levels, topics, special);
    }

    /** KAMP modu: kategorinin belirli grubunu (sabit dilim) dondur. group 0-tabanli. */
    @Transactional(readOnly = true)
    public List<StudyItemResponse> group(Long userId, String category, int group, int size) {
        int sz = Math.min(Math.max(size, 1), 60);
        int g = Math.max(group, 0);
        String src = null, cefr = null, topic = null;
        if (category != null && category.contains(":")) {
            String type = category.substring(0, category.indexOf(':'));
            String val = category.substring(category.indexOf(':') + 1).trim();
            switch (type) {
                case "level" -> { cefr = val; src = "CEFR"; }
                case "topic" -> { topic = val; src = "CEFR"; }
                case "source" -> src = val;
                default -> { }
            }
        }
        List<Long> ids = entryRepository.findEntryIdsByCategory(src, cefr, topic, PageRequest.of(g, sz));
        if (ids.isEmpty()) return List.of();
        Map<Long, DictionaryEntry> byId = new LinkedHashMap<>();
        for (DictionaryEntry e : entryRepository.findAllById(ids)) byId.put(e.getId(), e);
        List<StudyItemResponse> items = new ArrayList<>();
        for (Long id : ids) {
            DictionaryEntry e = byId.get(id);
            if (e != null) items.add(toItem(e, false));
        }
        return items;
    }

    /** YANLISLARIM: lapses>0 veya son sonuc AGAIN olan kelimeler. */
    @Transactional(readOnly = true)
    public List<StudyItemResponse> mistakes(Long userId, int count) {
        int want = Math.min(Math.max(count, 1), 60);
        List<Long> ids = progressRepository.findMistakeEntryIds(userId, PageRequest.of(0, want));
        if (ids.isEmpty()) return List.of();
        Map<Long, DictionaryEntry> byId = new LinkedHashMap<>();
        for (DictionaryEntry e : entryRepository.findAllById(ids)) byId.put(e.getId(), e);
        List<StudyItemResponse> items = new ArrayList<>();
        for (Long id : ids) {
            DictionaryEntry e = byId.get(id);
            if (e != null) items.add(toItem(e, false));
        }
        return items;
    }

    private StudyItemResponse toItem(DictionaryEntry e, boolean isNew) {
        DictionarySense first = e.getSenses().stream()
                .min(Comparator.comparingInt(s -> s.getCommonRank() != null ? s.getCommonRank() : s.getSenseOrder()))
                .orElse(null);
        String pos = null, defEn = null, meaningTr = null;
        if (first != null) {
            pos = first.getPos();
            defEn = first.getDefinition();
            meaningTr = first.getTranslations().stream()
                    .filter(t -> "tr".equalsIgnoreCase(t.getTargetLang()))
                    .map(DictionarySenseTr::getDefinitionTr)
                    .findFirst().orElse(null);
        }
        return new StudyItemResponse(e.getId(), e.getWord(), e.getCefr(), pos, meaningTr, defEn, isNew);
    }

    /** Cevabi isle: SM-2 ile katsayi/aralik/due guncelle. */
    @Transactional
    public void answer(Long userId, Long entryId, String ratingRaw) {
        String rating = (ratingRaw == null ? "GOOD" : ratingRaw.trim().toUpperCase());
        UserStudyProgress p = progressRepository.findByUserIdAndEntryId(userId, entryId)
                .orElseGet(() -> UserStudyProgress.builder()
                        .userId(userId).entryId(entryId)
                        .ease(EASE_START).intervalDays(0).reps(0).lapses(0)
                        .build());

        int interval;
        double ease = p.getEase();
        int reps = p.getReps();

        switch (rating) {
            case "AGAIN" -> {
                p.setLapses(p.getLapses() + 1);
                reps = 0;
                ease = Math.max(EASE_MIN, ease - 0.20);
                interval = 1;
            }
            case "HARD" -> {
                ease = Math.max(EASE_MIN, ease - 0.15);
                interval = (reps == 0) ? 1 : Math.max(1, (int) Math.round(p.getIntervalDays() * 1.2));
                reps++;
            }
            case "EASY" -> {
                ease = ease + 0.15;
                interval = (reps == 0) ? 2 : Math.max(1, (int) Math.round(p.getIntervalDays() * ease * 1.3));
                reps++;
            }
            default -> { // GOOD
                rating = "GOOD";
                if (reps == 0) interval = 1;
                else if (reps == 1) interval = 3;
                else interval = Math.max(1, (int) Math.round(p.getIntervalDays() * ease));
                reps++;
            }
        }

        p.setEase(ease);
        p.setReps(reps);
        p.setIntervalDays(interval);
        p.setDueDate(LocalDate.now().plusDays(interval));
        p.setLastResult(rating);
        p.setLastReviewedAt(LocalDateTime.now());
        progressRepository.save(p);
    }

    @Transactional(readOnly = true)
    public StudyStatsResponse stats(Long userId) {
        long due = progressRepository.countByUserIdAndDueDateLessThanEqual(userId, LocalDate.now());
        long studied = progressRepository.countByUserId(userId);
        long pool = entryRepository.count();
        return new StudyStatsResponse(due, studied, pool);
    }
}
