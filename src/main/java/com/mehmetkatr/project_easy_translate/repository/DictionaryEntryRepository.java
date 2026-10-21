package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.DictionaryEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DictionaryEntryRepository extends JpaRepository<DictionaryEntry, Long> {

    Optional<DictionaryEntry> findByWordIgnoreCaseAndLanguageCodeIgnoreCase(String word, String languageCode);

    boolean existsByWordIgnoreCaseAndLanguageCodeIgnoreCase(String word, String languageCode);

    /** Toplu on-ceviri icin tum id'ler, CEFR'e gore (A1 once) sirali. */
    @Query("SELECT e.id FROM DictionaryEntry e ORDER BY e.cefr ASC, e.id ASC")
    List<Long> findAllIdsByCefr();

    /** Kullanicinin HENUZ calismadigi kelime id'leri (quiz'e yeni eklenecekler), A1 once. */
    @Query("""
            SELECT e.id FROM DictionaryEntry e
            WHERE e.id NOT IN (SELECT p.entryId FROM UserStudyProgress p WHERE p.userId = :uid)
              AND (:exclude IS NULL OR e.id NOT IN :exclude)
              AND (:source IS NULL OR e.source = :source)
              AND (:cefr IS NULL OR e.cefr = :cefr)
              AND (:topic IS NULL OR e.topic = :topic)
            ORDER BY e.cefr ASC, e.id ASC
            """)
    List<Long> findNewEntryIds(@Param("uid") Long uid,
                               @Param("exclude") List<Long> exclude,
                               @Param("source") String source,
                               @Param("cefr") String cefr,
                               @Param("topic") String topic,
                               Pageable pageable);

    long countBySource(String source);

    /** Kamp modu: kategori icindeki kelimeler SABIT sirayla (grup dilimleme icin). */
    @Query("""
            SELECT e.id FROM DictionaryEntry e
            WHERE (:source IS NULL OR e.source = :source)
              AND (:cefr IS NULL OR e.cefr = :cefr)
              AND (:topic IS NULL OR e.topic = :topic)
            ORDER BY e.id ASC
            """)
    List<Long> findEntryIdsByCategory(@Param("source") String source,
                                      @Param("cefr") String cefr,
                                      @Param("topic") String topic,
                                      Pageable pageable);

    /** Kategori sayilari: kaynaga gore CEFR seviyesi dagilimi. */
    @Query("SELECT e.cefr, COUNT(e) FROM DictionaryEntry e WHERE e.source = :source AND e.cefr IS NOT NULL AND e.cefr <> '' GROUP BY e.cefr ORDER BY e.cefr")
    List<Object[]> countByCefr(@Param("source") String source);

    /** Kategori sayilari: kaynaga gore topic dagilimi. */
    @Query("SELECT e.topic, COUNT(e) FROM DictionaryEntry e WHERE e.source = :source AND e.topic IS NOT NULL AND e.topic <> '' GROUP BY e.topic ORDER BY COUNT(e) DESC")
    List<Object[]> countByTopic(@Param("source") String source);
}
