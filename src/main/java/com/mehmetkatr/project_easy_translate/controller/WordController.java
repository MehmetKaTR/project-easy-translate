package com.mehmetkatr.project_easy_translate.controller;

import com.mehmetkatr.project_easy_translate.dto.WordDTO;
import com.mehmetkatr.project_easy_translate.entity.User;
import com.mehmetkatr.project_easy_translate.entity.Word;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import com.mehmetkatr.project_easy_translate.service.WordListService;
import com.mehmetkatr.project_easy_translate.service.WordService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/words")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // frontend erişimi için  HOCA USERLAR ICIN TOKENLI KONTROL LAZIM SIMDILIK DEGIL DE UYGULAMAYI CIKARIRKEN LAZIM
public class WordController {

    private final WordService wordService;
    private final WordListService wordListService;

    /**
     * Belirli bir kullanıcıya ait TÜM kelimeleri döner.
     * Kullanıcı ID'si query param olarak gönderilir.
     * Örnek: GET /api/words/all?userId=1
     */
    @GetMapping("/all")
    public ResponseEntity<List<WordDTO>> getAllWordsByUser(@RequestParam Long userId) {

        List<Word> words = wordService.getAllWordsByUser(userId);

        // Sadece kullanıcıya ait kelimeleri WordDTO'ya çevir
        List<WordDTO> dtoList = words.stream()
                .map(WordDTO::new)
                .toList();

        // dtoList index'leri UI sırasına göre sıralı olacak şekilde
        // Örn: kelime eklenme zamanına göre veya wordlist sırasına göre sort edebilirsin

        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/byWordList")
    @Transactional
    public ResponseEntity<List<WordDTO>> getAllWordsByWordList(@RequestParam Long userId, @RequestParam Long wordListId) {

        User user = new User();
        user.setId(userId);

        Optional<WordList> wordListOpt = wordListService.getWordListsByUserAndWordListId(user, wordListId);

        if (wordListOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        WordList wordList = wordListOpt.get();

        // WordList içindeki kelimeleri DTO'ya çevir
        List<WordDTO> dtoList = wordList.getWords().stream()
                .map(WordDTO::new)
                .toList();

        return ResponseEntity.ok(dtoList);
    }


    @PutMapping("/add")
    public ResponseEntity<WordDTO> addWord(@RequestParam Long userId, @RequestBody WordDTO wordDTO) {
        User user = new User();
        user.setId(userId);

        Optional<WordList> temp = wordListService.getWordListsByUserAndName(user, "All");

        // 3. WordDTO -> Word entity
        Word word = Word.builder()
                .word(wordDTO.getWord())
                .translated(wordDTO.getTranslated())
                .languageCode(wordDTO.getLanguageCode())
                .wordList(temp.orElse(null))
                .build();

        // 4. Save
        Word saved = wordService.addWord(word);
        return ResponseEntity.ok(new WordDTO(saved));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<WordDTO> deleteWord(@RequestParam Long userId, @RequestParam Long wordId) {
        User user = new User();
        user.setId(userId);

        // Kullanıcının tüm WordList'lerini al
        List<WordList> wordLists = wordListService.getWordListsByUser(user);

        if (wordLists == null || wordLists.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Tüm listelerdeki kelimeleri stream ile birleştirip ID kontrolü yap
        Optional<Word> wordOpt = wordLists.stream()
                .flatMap(wl -> wordService.getWordsByWordList(wl).stream())
                .filter(w -> w.getId().equals(wordId))
                .findFirst();

        if (wordOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Word word = wordOpt.get();
        wordService.deleteWordById(word.getId());

        return ResponseEntity.ok(new WordDTO(word));
    }

}
