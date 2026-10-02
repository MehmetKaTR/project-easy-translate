package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.dto.response.AdminWordSummaryResponse;
import com.mehmetkatr.project_easy_translate.entity.Word;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WordRepository extends JpaRepository<Word, Long> {

    Optional<Word> findByTextIgnoreCaseAndLanguageCodeIgnoreCase(String text, String languageCode);

    List<Word> findByTextContainingIgnoreCase(String text);

    // Admin: global kelimeler + kaç kullanıcıda olduğu (usageCount), arama + en çok kullanılana göre.
    @Query("""
            SELECT new com.mehmetkatr.project_easy_translate.dto.response.AdminWordSummaryResponse(
                w.id, w.text, w.languageCode, w.blocked, COUNT(uw))
            FROM Word w
            LEFT JOIN UserWord uw ON uw.word = w
            WHERE (:q IS NULL OR LOWER(w.text) LIKE LOWER(CONCAT('%', :q, '%')))
            GROUP BY w.id, w.text, w.languageCode, w.blocked
            ORDER BY COUNT(uw) DESC, w.text ASC
            """)
    List<AdminWordSummaryResponse> searchAdminWords(@Param("q") String q, Pageable pageable);
}
