package com.miproyecto.appfinanciera.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class ClaudeAIService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAIService.class);
    private static final String MODEL = "claude-haiku-4-5-20251001";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${anthropic.api.key:}")
    private String apiKey;

    public ClaudeAIService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.anthropic.com")
                .build();
    }

    public String preguntar(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            return "⚠️ Configura tu API key de Claude en application.properties (anthropic.api.key).";
        }
        try {
            Map<String, Object> body = Map.of(
                    "model", MODEL,
                    "max_tokens", 512,
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            );

            String response = restClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return objectMapper.readTree(response)
                    .path("content").get(0).path("text").asText();
        } catch (Exception e) {
            log.error("Error llamando a Claude API: {}", e.getMessage());
            return "No fue posible conectar con la IA en este momento. Intenta de nuevo.";
        }
    }
}
