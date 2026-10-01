package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.WordDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WordDetailRepository extends JpaRepository<WordDetail, Long> {

    Optional<WordDetail> findByWordIdAndTargetLanguage(Long wordId, String targetLanguage);
}
