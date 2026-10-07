package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.DictionaryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DictionaryEntryRepository extends JpaRepository<DictionaryEntry, Long> {

    Optional<DictionaryEntry> findByWordIgnoreCaseAndLanguageCodeIgnoreCase(String word, String languageCode);

    boolean existsByWordIgnoreCaseAndLanguageCodeIgnoreCase(String word, String languageCode);

    /** Toplu on-ceviri icin tum id'ler, CEFR'e gore (A1 once) sirali. */
    @Query("SELECT e.id FROM DictionaryEntry e ORDER BY e.cefr ASC, e.id ASC")
    List<Long> findAllIdsByCefr();
}
