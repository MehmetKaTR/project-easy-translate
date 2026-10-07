package com.mehmetkatr.project_easy_translate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Microsoft (Azure) Translator v3 istemcisi.
 * Tek cagrida COK metin cevirir (batch); hizli (~200ms), ucretsiz F0 katmani 2M karakter/ay.
 * Yapilandirma (.env):
 *   MS_TRANSLATOR_KEY=...        (Azure Translator anahtari)
 *   MS_TRANSLATOR_REGION=...     (or. westeurope; "global" degil, kaynagin bolgesi)
 *   MS_TRANSLATOR_ENDPOINT=https://api.cognitive.microsofttranslator.com  (varsayilan)
 */
@Component
@RequiredArgsConstructor
public class MicrosoftTranslatorClient {

    private static final Logger log = LoggerFactory.getLogger(MicrosoftTranslatorClient.class);

    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    @Value("${ms.translator.key:${MS_TRANSLATOR_KEY:}}")
    private String key;

    @Value("${ms.translator.region:${MS_TRANSLATOR_REGION:}}")
    private String region;

    @Value("${ms.translator.endpoint:${MS_TRANSLATOR_ENDPOINT:https://api.cognitive.microsofttranslator.com}}")
    private String endpoint;

    public boolean isConfigured() {
        return key != null && !key.isBlank();
    }

    /**
     * texts'i from->to cevirir. Donen liste girdiyle AYNI sirada/boyuttadir.
     * Hata/yapilandirma yoksa BOS liste doner (cagiran taraf karar verir).
     */
    public List<String> translate(List<String> texts, String from, String to) {
        if (!isConfigured() || texts == null || texts.isEmpty()) return List.of();

        String url = endpoint.replaceAll("/+$", "")
                + "/translate?api-version=3.0&from=" + from + "&to=" + to;

        List<Map<String, String>> body = new ArrayList<>(texts.size());
        for (String t : texts) body.add(Map.of("Text", t == null ? "" : t));

        try {
            String response = restClient.post()
                    .uri(url)
                    .header("Ocp-Apim-Subscription-Key", key)
                    .headers(h -> { if (region != null && !region.isBlank()) h.add("Ocp-Apim-Subscription-Region", region); })
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response == null ? "[]" : response);
            if (!root.isArray()) return List.of();
            List<String> out = new ArrayList<>(root.size());
            for (JsonNode item : root) {
                out.add(item.path("translations").path(0).path("text").asText(""));
            }
            return out;
        } catch (Exception e) {
            log.info("[ms-translator] cagri hatasi: {}", e.getMessage());
            return List.of();
        }
    }
}
