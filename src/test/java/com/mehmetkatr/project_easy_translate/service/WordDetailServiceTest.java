package com.mehmetkatr.project_easy_translate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mehmetkatr.project_easy_translate.dto.response.WordDetailResponse;
import com.mehmetkatr.project_easy_translate.entity.Word;
import com.mehmetkatr.project_easy_translate.entity.WordDetail;
import com.mehmetkatr.project_easy_translate.repository.WordDetailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WordDetailServiceTest {

    @Mock
    private WordDetailRepository wordDetailRepository;

    @Mock
    private FreeDictionaryClient freeDictionaryClient;

    @Mock
    private SentenceExampleClient sentenceExampleClient;

    @Mock
    private TranslationSuggestionService translationSuggestionService;

    @Mock
    private GeminiClient geminiClient;

    private WordDetailService service;

    @BeforeEach
    void setUp() {
        service = new WordDetailService(
                wordDetailRepository,
                freeDictionaryClient,
                sentenceExampleClient,
                translationSuggestionService,
                geminiClient,
                new ObjectMapper());
    }

    @Test
    void cacheVarsaDisServisCagrilmaz() {
        Word word = Word.builder().id(5L).text("crack").languageCode("en").build();
        WordDetail cached = WordDetail.builder()
                .word(word)
                .targetLanguage("tr")
                .partOfSpeech("noun")
                .definition("def")
                .synonyms("[\"fissure\"]")
                .examples("[{\"sentence\":\"x\",\"translation\":\"y\"}]")
                .source("DICTIONARY")
                .build();
        when(wordDetailRepository.findByWordIdAndTargetLanguage(5L, "tr"))
                .thenReturn(Optional.of(cached));

        WordDetailResponse res = service.getOrCreate(word, "tr");

        assertThat(res.definition()).isEqualTo("def");
        assertThat(res.source()).isEqualTo("DICTIONARY");
        assertThat(res.synonyms()).containsExactly("fissure");
        assertThat(res.examples()).hasSize(1);
        verify(freeDictionaryClient, never()).lookup(any(), any());
        verify(geminiClient, never()).generate(any());
        verify(wordDetailRepository, never()).save(any());
    }

    @Test
    void cacheYoksaSozluktenUretir() {
        Word word = Word.builder().id(5L).text("crack").languageCode("en").build();
        when(wordDetailRepository.findByWordIdAndTargetLanguage(5L, "tr")).thenReturn(Optional.empty());
        when(freeDictionaryClient.lookup("crack", "en"))
                .thenReturn(Optional.of(new FreeDictionaryClient.Result(
                        "noun", "def", List.of("He has a crack"), List.of("fissure"))));
        when(translationSuggestionService.suggest(any(), any(), any())).thenReturn("ceviri");
        when(wordDetailRepository.save(any(WordDetail.class))).thenAnswer(i -> i.getArgument(0));

        WordDetailResponse res = service.getOrCreate(word, "tr");

        assertThat(res.source()).isEqualTo("FREE_DICTIONARY");
        assertThat(res.definition()).isEqualTo("def");
        assertThat(res.examples().get(0).translation()).isEqualTo("ceviri");
        verify(geminiClient, never()).generate(any());
    }

    @Test
    void cacheYoksaVeSozlukBulamazsaGeminiyeDuser() {
        Word word = Word.builder().id(7L).text("qwertyz").languageCode("en").build();
        when(wordDetailRepository.findByWordIdAndTargetLanguage(7L, "tr")).thenReturn(Optional.empty());
        when(freeDictionaryClient.lookup("qwertyz", "en")).thenReturn(Optional.empty());
        when(geminiClient.generate(any())).thenReturn(
                "{\"partOfSpeech\":\"noun\",\"definition\":\"made up\",\"synonyms\":[],"
                        + "\"examples\":[{\"sentence\":\"a qwertyz\",\"translation\":\"bir qwertyz\"}]}");
        when(wordDetailRepository.save(any(WordDetail.class))).thenAnswer(i -> i.getArgument(0));

        WordDetailResponse res = service.getOrCreate(word, "tr");

        assertThat(res.source()).isEqualTo("GEMINI");
        assertThat(res.definition()).isEqualTo("made up");
        assertThat(res.examples().get(0).translation()).isEqualTo("bir qwertyz");
        verify(geminiClient).generate(any());
    }
}
