package com.mehmetkatr.project_easy_translate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class GoogleTokenVerifierService {

    private final ObjectMapper objectMapper;

    @Value("${google.oauth.client-ids:}")
    private String allowedClientIdsCsv;

    public VerifiedGoogleUser verifyIdToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new IllegalArgumentException("Google id_token is required");
        }

        try {
            String body = RestClient.create()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("oauth2.googleapis.com")
                            .path("/tokeninfo")
                            .queryParam("id_token", idToken)
                            .build())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
            String email = root.path("email").asText("");
            String emailVerified = root.path("email_verified").asText("");
            String audience = root.path("aud").asText("");
            String name = root.path("name").asText("");

            if (email.isBlank()) {
                throw new IllegalArgumentException("Google token does not contain email");
            }

            if (!"true".equalsIgnoreCase(emailVerified) && !"1".equals(emailVerified)) {
                throw new IllegalArgumentException("Google email is not verified");
            }

            List<String> allowedClientIds = parseAllowedClientIds();
            if (allowedClientIds.isEmpty()) {
                throw new IllegalArgumentException("Server google client ids are not configured");
            }

            if (!allowedClientIds.contains(audience)) {
                throw new IllegalArgumentException("Google token audience is not allowed");
            }

            String username = deriveUsername(email, name);
            return new VerifiedGoogleUser(email, username);
        } catch (HttpClientErrorException ex) {
            throw new IllegalArgumentException("Google token is invalid or expired");
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Google token verification failed");
        }
    }

    private List<String> parseAllowedClientIds() {
        return Arrays.stream(allowedClientIdsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private String deriveUsername(String email, String name) {
        String candidate = (name == null || name.isBlank())
                ? email.substring(0, email.indexOf("@"))
                : name;

        candidate = candidate.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "");
        if (candidate.length() < 3) {
            candidate = "user" + Math.abs(email.hashCode());
        }
        if (candidate.length() > 50) {
            candidate = candidate.substring(0, 50);
        }
        return candidate;
    }

    public record VerifiedGoogleUser(String email, String username) {}
}
