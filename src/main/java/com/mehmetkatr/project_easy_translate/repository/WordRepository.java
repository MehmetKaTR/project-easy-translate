package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.Word;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WordRepository extends JpaRepository<Word, Long> {

    List<Word> findByWordList(WordList wordList);

    Optional<Word> findByWord(String word);

    List<Word> findByTranslated(String translated);

    List<Word> findByLanguageCode(String languageCode);

    List<Word> findByWordListAndLanguageCode(WordList wordList, String languageCode);

    List<Word> findByWordListAndTranslated(WordList wordList, String translated);

    List<Word> findByWordListAndStarred(WordList wordList, boolean starred);

    @Query("SELECT w FROM Word w WHERE w.wordList.user.id = :userId ORDER BY w.id ASC")
    List<Word> findAllByUserId(Long userId);

    @Query("SELECT w FROM Word w WHERE w.wordList.user.id = :userId AND w.starred = true ORDER BY w.id ASC")
    List<Word> findStarredByUserId(Long userId);
}
