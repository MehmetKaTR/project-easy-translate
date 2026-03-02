package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.Word;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WordRepository extends JpaRepository<Word, Long> {

    @Query("SELECT DISTINCT w FROM Word w JOIN w.wordLists wl WHERE wl = :wordList ORDER BY w.id ASC")
    List<Word> findByWordList(@Param("wordList") WordList wordList);

    Optional<Word> findByWord(String word);

    List<Word> findByTranslated(String translated);

    List<Word> findByLanguageCode(String languageCode);

    @Query("SELECT DISTINCT w FROM Word w JOIN w.wordLists wl WHERE wl = :wordList AND LOWER(w.languageCode) = LOWER(:languageCode) ORDER BY w.id ASC")
    List<Word> findByWordListAndLanguageCode(@Param("wordList") WordList wordList, @Param("languageCode") String languageCode);

    @Query("SELECT DISTINCT w FROM Word w JOIN w.wordLists wl WHERE wl = :wordList AND LOWER(w.translated) = LOWER(:translated) ORDER BY w.id ASC")
    List<Word> findByWordListAndTranslated(@Param("wordList") WordList wordList, @Param("translated") String translated);

    @Query("SELECT DISTINCT w FROM Word w JOIN w.wordLists wl WHERE wl = :wordList AND w.starred = :starred ORDER BY w.id ASC")
    List<Word> findByWordListAndStarred(@Param("wordList") WordList wordList, @Param("starred") boolean starred);

    @Query("SELECT w FROM Word w WHERE w.user.id = :userId ORDER BY w.id ASC")
    List<Word> findAllByUserId(Long userId);

    @Query("SELECT w FROM Word w WHERE w.user.id = :userId AND w.starred = true ORDER BY w.id ASC")
    List<Word> findStarredByUserId(Long userId);

    @Query("""
            SELECT w FROM Word w
            WHERE w.user.id = :userId
              AND LOWER(w.word) = LOWER(:word)
              AND LOWER(w.translated) = LOWER(:translated)
              AND LOWER(w.languageCode) = LOWER(:languageCode)
            ORDER BY w.id ASC
            """)
    Optional<Word> findFirstByUserAndContent(
            @Param("userId") Long userId,
            @Param("word") String word,
            @Param("translated") String translated,
            @Param("languageCode") String languageCode
    );
}
