package com.mehmetkatr.project_easy_translate.controller;

import com.mehmetkatr.project_easy_translate.dto.WordDTO;
import com.mehmetkatr.project_easy_translate.entity.User;
import com.mehmetkatr.project_easy_translate.entity.Word;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import com.mehmetkatr.project_easy_translate.service.WordListService;
import com.mehmetkatr.project_easy_translate.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/words")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // frontend erişimi için
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
        User user = new User();
        user.setId(userId);

        List<WordList> userLists = wordListService.getWordListsByUser(user);

        List<WordDTO> allWords = new ArrayList<>();
        for (WordList list : userLists) {
            List<Word> words = wordService.getWordsByWordList(list);
            words.forEach(w -> allWords.add(new WordDTO(w)));
        }

        return ResponseEntity.ok(allWords);
    }

}
