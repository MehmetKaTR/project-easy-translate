package com.mehmetkatr.project_easy_translate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mehmetkatr.project_easy_translate.dto.story.StoryGenerateRequest;
import com.mehmetkatr.project_easy_translate.dto.story.StoryGenerateResponse;
import com.mehmetkatr.project_easy_translate.dto.story.StoryLimitStatusResponse;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

        String selectedModel = null;
        String translationTarget = normalizeTranslationTarget(request.getTranslationTarget());
        String prompt = buildPrompt(request, translationTarget);
        String responseText = null;

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

        ensureActiveModelRecord(selectedModel);

        String rawJson = extractGeminiText(responseText);
        ParsedStory parsedStory = parseModelOutput(rawJson, translationTarget);
        int tokensUsed = extractTokensUsed(responseText);
        persistTokenUsage(user, tokensUsed);

        return StoryGenerateResponse.builder()
                .title(parsedStory.title())
                .story(parsedStory.storyEn())
                .turkishTranslation("tr".equals(translationTarget) ? parsedStory.storyTranslated() : "")
                .translatedStory(parsedStory.storyTranslated())
                .translatedLanguage(translationTarget)
                .model(selectedModel)
                .tokensUsed(tokensUsed)
                .build();
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

    private String buildPrompt(StoryGenerateRequest request, String translationTarget) {
        String words = request.getWords().stream()
                .map(StoryGenerateRequest.WordItem::getWord)
                .map(String::trim)
                .filter(w -> !w.isBlank())
                .collect(Collectors.joining(", "));

        String translationFieldDescription = switch (translationTarget) {
            case "de" -> "\"story_de\": \"German translation of the English story\"";
            case "es" -> "\"story_es\": \"Spanish translation of the English story\"";
            case "none" -> "";
            default -> "\"story_tr\": \"Turkish translation of the English story\"";
        };
        String translationRule = switch (translationTarget) {
            case "de" -> "- story_de must be natural German translation";
            case "es" -> "- story_es must be natural Spanish translation";
            case "none" -> "";
            default -> "- story_tr must be natural Turkish translation";
        };
        String outputSchema = translationTarget.equals("none")
                ? """
                {
                  "title": "max 8 words",
                  "story_en": "English story only"
                }
                """
                : """
                {
                  "title": "max 8 words",
                  "story_en": "English story only",
                  %s
                }
                """.formatted(translationFieldDescription);

        return """
                You are an English learning assistant.
                
                Write a story and return only valid JSON.
                
                Constraints:
                - CEFR level: %s
                - Topic: %s
                - Sentiment: %s
                - Length: %s
                - Must naturally include all words: %s
                
                Output JSON schema:
                %s
                
                Rules:
                - No markdown
                - No extra keys
                - story_en must be CEFR %s compatible
                - Include each required word exactly as given (same spelling, no inflection changes)
                %s
                """.formatted(
                request.getLevel(),
                request.getTopic(),
                request.getSentiment(),
                request.getLength(),
                words,
                outputSchema,
                request.getLevel()
                ,
                translationRule
        );
    }

    private String callGemini(String prompt, String modelName) {
        Map<String, Object> payload = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{Map.of("text", prompt)})
                },
                "generationConfig", Map.of(
                        "temperature", 0.7,
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

    private ParsedStory parseModelOutput(String rawJson, String translationTarget) {
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

            if (title.isBlank()) title = "New Story";
            return new ParsedStory(title, storyEn, translatedStory);
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

    private record ParsedStory(String title, String storyEn, String storyTranslated) {}
}
