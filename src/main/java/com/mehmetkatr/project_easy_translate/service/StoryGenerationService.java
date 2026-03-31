package com.mehmetkatr.project_easy_translate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mehmetkatr.project_easy_translate.dto.story.StoryGenerateRequest;
import com.mehmetkatr.project_easy_translate.dto.story.StoryGenerateResponse;
import com.mehmetkatr.project_easy_translate.dto.story.StoryLimitStatusResponse;
import com.mehmetkatr.project_easy_translate.dto.story.StoryWordValidationRequest;
import com.mehmetkatr.project_easy_translate.dto.story.StoryWordValidationResponse;
import com.mehmetkatr.project_easy_translate.entity.LlmModel;
import com.mehmetkatr.project_easy_translate.entity.TokenUsageLog;
import com.mehmetkatr.project_easy_translate.entity.User;
import com.mehmetkatr.project_easy_translate.exception.DailyStoryLimitExceededException;
import com.mehmetkatr.project_easy_translate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class StoryGenerationService {

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final TokenUsageLogService tokenUsageLogService;
    private final LlmModelService llmModelService;

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    @Value("${gemini.fallback-models:gemini-2.0-flash,gemini-2.0-flash-lite,gemini-1.5-flash-latest}")
    private String geminiFallbackModels;

    @Value("${gemini.api-version:v1beta}")
    private String geminiApiVersion;

    @Value("${app.story-limit.free-daily:3}")
    private int freeDailyStoryLimit;

    @Value("${app.story-limit.premium-daily:20}")
    private int premiumDailyStoryLimit;

    public StoryGenerateResponse generateStory(Long userId, StoryGenerateRequest request) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new IllegalArgumentException("Gemini API key is not configured");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        validateDailyStoryGenerationLimit(user);

        String translationTarget = normalizeTranslationTarget(request.getTranslationTarget());
        String prompt = buildPrompt(request, translationTarget);
        ModelCallResult modelCall = callGeminiWithFallback(prompt);

        ensureActiveModelRecord(modelCall.modelName());

        String rawJson = extractGeminiText(modelCall.responseText());
        ParsedStory parsedStory = parseModelOutput(rawJson, translationTarget, sanitizeWords(request.getWords()));
        int tokensUsed = extractTokensUsed(modelCall.responseText());
        persistTokenUsage(user, tokensUsed);

        return StoryGenerateResponse.builder()
                .title(parsedStory.title())
                .story(parsedStory.storyEn())
                .turkishTranslation("tr".equals(translationTarget) ? parsedStory.storyTranslated() : "")
                .translatedStory(parsedStory.storyTranslated())
                .translatedLanguage(translationTarget)
                .translatedWordsCsv(parsedStory.translatedWordsCsv())
                .wordMappings(parsedStory.wordMappings())
                .model(modelCall.modelName())
                .tokensUsed(tokensUsed)
                .build();
    }

    public StoryWordValidationResponse validateWords(StoryWordValidationRequest request) {
        List<StoryGenerateRequest.WordItem> words = sanitizeWords(request.getWords());
        if (words.isEmpty()) {
            return StoryWordValidationResponse.builder().items(List.of()).build();
        }

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return fallbackValidation(words);
        }

        try {
            String translationTarget = normalizeTranslationTarget(request.getTranslationTarget());
            ModelCallResult modelCall = callGeminiWithFallback(buildValidationPrompt(words, translationTarget));
            ensureActiveModelRecord(modelCall.modelName());
            String rawJson = extractGeminiText(modelCall.responseText());
            return parseValidationOutput(rawJson, words);
        } catch (Exception ex) {
            return fallbackValidation(words);
        }
    }

    public StoryLimitStatusResponse getDailyLimitStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        int dailyLimit = resolveDailyLimit(user.getSubscriptionLevel());
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime resetAt = today.plusDays(1).atStartOfDay();
        long usedTodayLong = tokenUsageLogService.countByUserBetween(user, start, resetAt);
        int usedToday = (int) Math.min(Integer.MAX_VALUE, usedTodayLong);
        int remainingToday = Math.max(0, dailyLimit - usedToday);

        return StoryLimitStatusResponse.builder()
                .dailyLimit(dailyLimit)
                .usedToday(usedToday)
                .remainingToday(remainingToday)
                .resetAt(resetAt.toString())
                .limitReached(usedToday >= dailyLimit)
                .plan(user.getSubscriptionLevel().name())
                .build();
    }

    private List<StoryGenerateRequest.WordItem> sanitizeWords(List<StoryGenerateRequest.WordItem> words) {
        if (words == null) return List.of();
        return words.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getWord() != null && !item.getWord().trim().isBlank())
                .toList();
    }

    private String clean(String value) {
        if (value == null) return "";
        return value.trim();
    }

    private String safeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private String resolvePreferredSuggestionLanguage(String translationTarget) {
        return switch (translationTarget) {
            case "tr" -> "simple Turkish";
            case "de" -> "simple German";
            case "es" -> "simple Spanish";
            default -> "simple English";
        };
    }

    private String buildValidationPrompt(List<StoryGenerateRequest.WordItem> words, String translationTarget) {
        List<Map<String, String>> payload = words.stream()
                .map(item -> {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("clientId", clean(item.getClientId()));
                    row.put("word", clean(item.getWord()));
                    row.put("hint", clean(item.getTranslated()));
                    row.put("languageCode", clean(item.getLanguageCode()));
                    return row;
                })
                .toList();

        String preferredSuggestionLanguage = resolvePreferredSuggestionLanguage(translationTarget);

        return """
                You validate meaning hints for English learning flashcards.

                Return only valid JSON.

                Each item contains:
                - word: the required source word
                - hint: the user's meaning hint; it may be in any language

                Decision rules:
                - status "ok" when the hint is a plausible meaning, synonym, or natural translation
                - status "ambiguous" when the hint is still plausible but only selects one valid sense of a multi-meaning word
                - status "suspicious" when the hint looks unrelated, nonsensical, person-name-like, or strongly conflicts with the word
                - hints in different languages are valid if the meaning matches
                - if hint is empty, mark status "ok" with empty approvedHint and suggestedHint
                - approvedHint is the short meaning hint that should guide story generation
                - for suspicious items, suggestedHint should be a better short meaning
                - suggestedHint must stay in the same language as the user's hint whenever possible
                - Never rewrite a non-English hint into English if you can infer the hint language
                - If the hint language is unclear, use %s for suggestedHint
                - Example: word "car", hint "ahmet" -> Turkish suggestedHint "araba"
                - reason must be short and user-friendly, maximum 8 words

                Input items:
                %s

                Output JSON schema:
                {
                  "items": [
                    {
                      "clientId": "same as input",
                      "word": "same source word",
                      "originalHint": "same original hint",
                      "status": "ok | ambiguous | suspicious",
                      "approvedHint": "short hint to use",
                      "suggestedHint": "better short hint or empty",
                      "reason": "short explanation"
                    }
                  ]
                }
                """.formatted(preferredSuggestionLanguage, safeJson(payload));
    }

    private StoryWordValidationResponse fallbackValidation(List<StoryGenerateRequest.WordItem> words) {
        return StoryWordValidationResponse.builder()
                .items(words.stream()
                        .map(item -> StoryWordValidationResponse.Item.builder()
                                .clientId(clean(item.getClientId()))
                                .word(clean(item.getWord()))
                                .originalHint(clean(item.getTranslated()))
                                .status("ok")
                                .approvedHint(clean(item.getTranslated()))
                                .suggestedHint("")
                                .reason("")
                                .build())
                        .toList())
                .build();
    }

    private String buildPrompt(StoryGenerateRequest request, String translationTarget) {
        List<StoryGenerateRequest.WordItem> words = sanitizeWords(request.getWords());
        int requiredWordCount = words.size();
        String translationFieldDescription = switch (translationTarget) {
            case "de" -> "\"story_de\": \"German translation of the English story\"";
            case "es" -> "\"story_es\": \"Spanish translation of the English story\"";
            case "none" -> "";
            default -> "\"story_tr\": \"Turkish translation of the English story\"";
        };

        String outputSchema = translationTarget.equals("none")
                ? """
                {
                  "title": "max 8 words",
                  "story_en": "English story only",
                  "word_mappings": [
                    {
                      "source_word": "required word exactly as given",
                      "source_hint": "meaning hint actually used, or empty",
                      "target_word": "",
                      "hint_status": "used | ignored | not_provided"
                    }
                  ]
                }
                """
                : """
                {
                  "title": "max 8 words",
                  "story_en": "English story only",
                  %s,
                  "word_mappings": [
                    {
                      "source_word": "required word exactly as given",
                      "source_hint": "meaning hint actually used, or empty",
                      "target_word": "natural translated equivalent in the target language",
                      "hint_status": "used | ignored | not_provided"
                    }
                  ]
                }
                """.formatted(translationFieldDescription);

        String requiredWordEntries = words.stream()
                .map(item -> {
                    String sourceWord = clean(item.getWord());
                    String hint = clean(item.getTranslated());
                    String languageCode = clean(item.getLanguageCode());
                    if (!hint.isBlank()) {
                        return "- source_word: \"%s\" | meaning_hint: \"%s\" | source_language: \"%s\""
                                .formatted(sourceWord, hint, languageCode.isBlank() ? "EN" : languageCode);
                    }
                    return "- source_word: \"%s\" | meaning_hint: \"\" | source_language: \"%s\""
                            .formatted(sourceWord, languageCode.isBlank() ? "EN" : languageCode);
                })
                .collect(Collectors.joining("\n"));

        String targetLanguageRule = switch (translationTarget) {
            case "de" -> "- target_word must be the natural German equivalent for the exact sense used";
            case "es" -> "- target_word must be the natural Spanish equivalent for the exact sense used";
            case "none" -> "- target_word must be an empty string";
            default -> "- target_word must be the natural Turkish equivalent for the exact sense used";
        };

        String targetWordPrecisionRule = translationTarget.equals("none")
                ? ""
                : """
                - target_word must exactly match the visible word or phrase that actually appears in the translated story
                - Do not return a dictionary lemma, normalized form, or alternative synonym if the translated story uses different wording
                - If meaning_hint is already written in the same language as the translation target and it is a natural translation for the chosen sense, prefer reusing that exact wording in the translated story
                """;

        return """
                You are an English learning assistant.

                Write a story and return only valid JSON.

                Translation target: %s

                Constraints:
                - CEFR level: %s
                - Topic: %s
                - Sentiment: %s
                - Length: %s

                Required word entries:
                %s

                Output JSON schema:
                %s

                Rules:
                - No markdown
                - No extra keys
                - story_en must be CEFR %s compatible
                - story_en must be natural English
                - Include each source_word exactly as given in story_en (same spelling, no inflection changes)
                - meaning_hint is only a semantic hint and may be in any language
                - If a meaning_hint is semantically valid, use the source_word in that sense
                - If a meaning_hint is nonsensical or unrelated, ignore it and use a common natural sense
                - word_mappings must contain exactly %s items in the same order as the required word entries
                - In word_mappings, source_word must exactly match the corresponding required source word
                - In word_mappings, source_hint must be the hint actually used, or empty if no hint was used
                %s
                - In word_mappings, hint_status must be one of: used, ignored, not_provided
                %s
                - Never URL-encode output text (do not use %%20, %%0A, or + for spaces)
                - Return valid JSON only
                """.formatted(
                translationTarget,
                request.getLevel(),
                request.getTopic(),
                request.getSentiment(),
                request.getLength(),
                requiredWordEntries,
                outputSchema,
                request.getLevel(),
                requiredWordCount,
                targetWordPrecisionRule,
                targetLanguageRule
        );
    }

    private ModelCallResult callGeminiWithFallback(String prompt) {
        String responseText = null;
        String selectedModel = null;
        HttpClientErrorException.TooManyRequests lastQuotaError = null;

        for (String modelCandidate : resolveModelCandidates()) {
            try {
                responseText = callGemini(prompt, modelCandidate);
                selectedModel = modelCandidate;
                break;
            } catch (HttpClientErrorException.NotFound ex) {
                // Try next model candidate
            } catch (HttpClientErrorException.TooManyRequests ex) {
                lastQuotaError = ex;
            }
        }

        if (responseText == null || selectedModel == null) {
            if (lastQuotaError != null) {
                throw new IllegalArgumentException("Gemini quota exceeded for all configured models. Try again later or change model/key.");
            }
            throw new IllegalArgumentException("No compatible Gemini model found. Update GEMINI_MODEL/GEMINI_FALLBACK_MODELS.");
        }

        return new ModelCallResult(selectedModel, responseText);
    }

    private String callGemini(String prompt, String modelName) {
        Map<String, Object> payload = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{Map.of("text", prompt)})
                },
                "generationConfig", Map.of(
                        "temperature", 0.45,
                        "responseMimeType", "application/json"
                )
        );

        return RestClient.create()
                .post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("generativelanguage.googleapis.com")
                        .path("/" + geminiApiVersion + "/models/" + modelName + ":generateContent")
                        .queryParam("key", geminiApiKey)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);
    }

    private List<String> resolveModelCandidates() {
        return Arrays.stream((geminiModel + "," + geminiFallbackModels).split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
    }

    private String extractGeminiText(String responseText) {
        try {
            JsonNode root = objectMapper.readTree(responseText == null ? "{}" : responseText);
            return root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
        } catch (Exception ex) {
            throw new IllegalArgumentException("Gemini response parsing failed");
        }
    }

    private String nodeText(JsonNode node, String... keys) {
        if (node == null || node.isMissingNode() || node.isNull()) return "";
        for (String key : keys) {
            String value = node.path(key).asText("").trim();
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String sanitizeHintStatus(String value, String sourceHint) {
        String normalized = clean(value).toLowerCase();
        if ("used".equals(normalized) || "ignored".equals(normalized) || "not_provided".equals(normalized)) {
            return normalized;
        }
        return clean(sourceHint).isBlank() ? "not_provided" : "used";
    }

    private String sanitizeValidationStatus(String value) {
        String normalized = clean(value).toLowerCase();
        return switch (normalized) {
            case "ambiguous", "suspicious" -> normalized;
            default -> "ok";
        };
    }

    private StoryWordValidationResponse parseValidationOutput(String rawJson, List<StoryGenerateRequest.WordItem> originalWords) {
        try {
            JsonNode root = objectMapper.readTree(rawJson == null ? "{}" : rawJson);
            JsonNode itemsNode = root.path("items");
            if (!itemsNode.isArray()) {
                return fallbackValidation(originalWords);
            }

            List<StoryWordValidationResponse.Item> items = new ArrayList<>();
            for (int index = 0; index < originalWords.size(); index += 1) {
                StoryGenerateRequest.WordItem original = originalWords.get(index);
                JsonNode itemNode = index < itemsNode.size() ? itemsNode.get(index) : null;
                String originalHint = clean(original.getTranslated());
                String status = sanitizeValidationStatus(nodeText(itemNode, "status"));
                String approvedHint = clean(nodeText(itemNode, "approvedHint", "approved_hint"));
                String suggestedHint = clean(nodeText(itemNode, "suggestedHint", "suggested_hint"));
                String reason = clean(nodeText(itemNode, "reason"));

                if ("ok".equals(status) && approvedHint.isBlank()) {
                    approvedHint = originalHint;
                }
                if ("ambiguous".equals(status) && approvedHint.isBlank()) {
                    approvedHint = !originalHint.isBlank() ? originalHint : suggestedHint;
                }
                if ("suspicious".equals(status) && approvedHint.isBlank()) {
                    approvedHint = suggestedHint;
                }
                if (reason.isBlank()) {
                    reason = switch (status) {
                        case "ambiguous" -> "Meaning is valid but sense-specific.";
                        case "suspicious" -> "Meaning hint looks unrelated.";
                        default -> "";
                    };
                }

                items.add(StoryWordValidationResponse.Item.builder()
                        .clientId(clean(original.getClientId()))
                        .word(clean(original.getWord()))
                        .originalHint(originalHint)
                        .status(status)
                        .approvedHint(approvedHint)
                        .suggestedHint(suggestedHint)
                        .reason(reason)
                        .build());
            }

            return StoryWordValidationResponse.builder()
                    .items(items)
                    .build();
        } catch (Exception ex) {
            return fallbackValidation(originalWords);
        }
    }

    private ParsedStory parseModelOutput(String rawJson, String translationTarget, List<StoryGenerateRequest.WordItem> requestWords) {
        try {
            JsonNode root = objectMapper.readTree(rawJson == null ? "{}" : rawJson);
            String title = root.path("title").asText("New Story").trim();
            String storyEn = root.path("story_en").asText("").trim();
            String translationField = switch (translationTarget) {
                case "de" -> "story_de";
                case "es" -> "story_es";
                case "none" -> "";
                default -> "story_tr";
            };
            String translatedStory = translationField.isBlank() ? "" : root.path(translationField).asText("").trim();

            if (storyEn.isBlank()) {
                throw new IllegalArgumentException("Gemini returned empty story");
            }

            List<StoryGenerateResponse.WordMapping> wordMappings = new ArrayList<>();
            JsonNode mappingsNode = root.path("word_mappings");
            for (int index = 0; index < requestWords.size(); index += 1) {
                StoryGenerateRequest.WordItem requestWord = requestWords.get(index);
                JsonNode mappingNode = mappingsNode.isArray() && index < mappingsNode.size() ? mappingsNode.get(index) : null;
                String sourceWord = clean(nodeText(mappingNode, "source_word", "sourceWord", "word"));
                if (sourceWord.isBlank()) {
                    sourceWord = clean(requestWord.getWord());
                }
                String sourceHint = clean(nodeText(mappingNode, "source_hint", "sourceHint", "hint"));
                String originalHint = clean(requestWord.getTranslated());
                if (sourceHint.isBlank()) {
                    sourceHint = originalHint;
                }
                String targetWord = clean(nodeText(mappingNode, "target_word", "targetWord", "translated_word", "translatedWord"));
                if (!translatedStory.isBlank() && !containsWholePhrase(translatedStory, targetWord) && containsWholePhrase(translatedStory, sourceHint)) {
                    targetWord = sourceHint;
                }
                String hintStatus = sanitizeHintStatus(nodeText(mappingNode, "hint_status", "hintStatus"), sourceHint);

                wordMappings.add(StoryGenerateResponse.WordMapping.builder()
                        .sourceWord(sourceWord)
                        .sourceHint(sourceHint)
                        .targetWord(targetWord)
                        .hintStatus(hintStatus)
                        .build());
            }

            String translatedWordsCsv = wordMappings.stream()
                    .map(StoryGenerateResponse.WordMapping::getTargetWord)
                    .map(this::clean)
                    .filter(value -> !value.isBlank())
                    .collect(Collectors.joining(","));

            if (translatedWordsCsv.isBlank()) {
                translatedWordsCsv = root.path("translated_words_csv").asText("").trim();
            }

            if (title.isBlank()) title = "New Story";
            return new ParsedStory(title, storyEn, translatedStory, translatedWordsCsv, wordMappings);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Gemini output is not valid JSON");
        }
    }

    private int extractTokensUsed(String responseText) {
        try {
            JsonNode root = objectMapper.readTree(responseText == null ? "{}" : responseText);
            JsonNode usage = root.path("usageMetadata");
            int total = usage.path("totalTokenCount").asInt(0);
            if (total > 0) return total;
            return usage.path("promptTokenCount").asInt(0) + usage.path("candidatesTokenCount").asInt(0);
        } catch (Exception ex) {
            return 0;
        }
    }

    private void persistTokenUsage(User user, int tokensUsed) {
        TokenUsageLog log = TokenUsageLog.builder()
                .user(user)
                .storyId("GEN-" + UUID.randomUUID())
                .tokensUsed(Math.max(tokensUsed, 0))
                .build();
        tokenUsageLogService.save(log);
    }

    private void validateDailyStoryGenerationLimit(User user) {
        int dailyLimit = resolveDailyLimit(user.getSubscriptionLevel());
        if (dailyLimit <= 0) return;

        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime resetAt = today.plusDays(1).atStartOfDay();
        long usedToday = tokenUsageLogService.countByUserBetween(user, start, resetAt);

        if (usedToday >= dailyLimit) {
            String resetAtText = resetAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            throw new DailyStoryLimitExceededException(
                    "Gunluk hikaye limiti doldu (" + usedToday + "/" + dailyLimit + "). "
                            + "Bir sonraki hak yenilenme zamani: " + resetAtText + ". Premium'a gec.",
                    dailyLimit,
                    (int) usedToday,
                    resetAt
            );
        }
    }

    private int resolveDailyLimit(User.SubscriptionLevel subscriptionLevel) {
        if (subscriptionLevel == User.SubscriptionLevel.PREMIUM) {
            return premiumDailyStoryLimit;
        }
        return freeDailyStoryLimit;
    }

    private boolean containsWholePhrase(String text, String phrase) {
        String normalizedText = clean(text);
        String normalizedPhrase = clean(phrase);
        if (normalizedText.isBlank() || normalizedPhrase.isBlank()) return false;

        String regex = "(?iu)(?<![\\p{L}\\p{N}])" + Pattern.quote(normalizedPhrase) + "(?![\\p{L}\\p{N}])";
        return Pattern.compile(regex).matcher(normalizedText).find();
    }

    private void ensureActiveModelRecord(String modelName) {
        boolean exists = llmModelService.findByName(modelName).stream()
                .anyMatch(model -> model.getStatus() == LlmModel.Status.ACTIVE);
        if (exists) return;

        llmModelService.save(LlmModel.builder()
                .name(modelName)
                .description("Gemini model for story generation")
                .status(LlmModel.Status.ACTIVE)
                .build());
    }

    private String normalizeTranslationTarget(String input) {
        String value = input == null ? "" : input.trim().toLowerCase();
        return switch (value) {
            case "tr", "de", "es", "none" -> value;
            default -> "tr";
        };
    }

    private record ModelCallResult(String modelName, String responseText) {}

    private record ParsedStory(
            String title,
            String storyEn,
            String storyTranslated,
            String translatedWordsCsv,
            List<StoryGenerateResponse.WordMapping> wordMappings
    ) {}
}
