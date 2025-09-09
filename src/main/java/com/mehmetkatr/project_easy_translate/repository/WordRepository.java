package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.Word;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.mongodb.core.mapping.Language;

import java.util.List;
import java.util.Optional;

public interface WordRepository extends JpaRepository<Word, Long> {

    List<Word> findByWordList(WordList wordList);

    Optional<Word> findByWord(Word word);

    List<Word> findByTranslated(String translated);

    List<Word> findByLanguageCode(String languageCode);

    List<Word> findByWordListAndLanguageCode(WordList wordList, String languageCode);

}
