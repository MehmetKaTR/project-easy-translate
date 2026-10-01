package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.Word;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WordRepository extends JpaRepository<Word, Long> {

    Optional<Word> findByTextIgnoreCaseAndLanguageCodeIgnoreCase(String text, String languageCode);

    List<Word> findByTextContainingIgnoreCase(String text);
}
