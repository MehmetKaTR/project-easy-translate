package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.TranslationCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TranslationCacheRepository extends JpaRepository<TranslationCache, Long> {

    Optional<TranslationCache> findBySourceTextAndSourceLangAndTargetLang(
            String sourceText, String sourceLang, String targetLang);
}
