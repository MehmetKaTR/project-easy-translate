package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.WordDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WordDetailRepository extends JpaRepository<WordDetail, Long> {

    Optional<WordDetail> findByWordIdAndTargetLanguage(Long wordId, String targetLanguage);

    @Modifying
    @Query(value = "DELETE FROM word_details WHERE word_id = :wordId", nativeQuery = true)
    int deleteByWordIdNative(@Param("wordId") Long wordId);
}
