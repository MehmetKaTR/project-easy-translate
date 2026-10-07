package com.mehmetkatr.project_easy_translate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * MyMemory çeviri istemcisi — ANAHTARSIZ, ücretsiz.
 * Tek çağrıda bir metin çevirir (q=...); cümle/tanım kalitesi iyi.
 * Kota: anonim ~1000 istek/gün; MYMEMORY_EMAIL verilirse ~50k kelime/gün.
 * Not: her (metin, dil) bir kez çevrilip DB'ye cache'lendiginden gercek cagri sayisi cok dusuk.
 */
@Component
@RequiredArgsConstructor
public class MyMemoryTranslatorClient {

    private static final Logger log = LoggerFactory.getLogger(MyMemoryTranslatorClient.class);

    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    @Value("${mymemory.email:${MYMEMORY_EMAIL:}}")
    private String email;

    /** texts'i from->to cevirir; girdiyle AYNI sira/boyutta liste doner (cevrilemeyen bos string). */
    public List<String> translate(List<String> texts, String from, String to) {
        List<String> out = new ArrayList<>(texts == null ? 0 : texts.size());
        if (texts == null) return out;
        for (String t : texts) out.add(translateOne(t, from, to));
        return out;
    }

    public String translateOne(String text, String from, String to) {
        if (text == null || text.isBlank()) return "";
        try {
            String q = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = "https://api.mymemory.translated.net/get?q=" + q
                    + "&langpair=" + from + "%7C" + to;
            if (email != null && !email.isBlank()) {
                url += "&de=" + URLEncoder.encode(email, StandardCharsets.UTF_8);
            }
            // Not: uri(String) onceden encode edilmis %7C'yi tekrar encode eder -> URI.create ile gonder.
            String response = restClient.get().uri(java.net.URI.create(url)).retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
            String tr = root.path("responseData").path("translatedText").asText("");
            // MyMemory bazen kota/uyari/hata metnini translatedText'e koyar -> ele
            String up = tr.toUpperCase();
            if (up.contains("MYMEMORY WARNING") || up.contains("QUERY LENGTH LIMIT")
                    || up.contains("INVALID LANGUAGE PAIR") || up.startsWith("'")) {
                return "";
            }
            return tr.trim();
        } catch (Exception e) {
            log.info("[mymemory] cagri hatasi: {}", e.getMessage());
            return "";
        }
    }
}
