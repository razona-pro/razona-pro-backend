// src/main/java/com/razonapro/razonaprobackend/infrastructure/ai/CloudChatClient.java
package com.razonapro.razonaprobackend.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razonapro.razonaprobackend.infrastructure.config.AiModelProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "ai.model.provider", havingValue = "CLOUD")
public class CloudChatClient implements ChatClient {

    private final AiModelProperties props;
    private final ObjectMapper      mapper;
    private final HttpClient        httpClient;

    public CloudChatClient(AiModelProperties props, ObjectMapper mapper) {
        this.props  = props;
        this.mapper = mapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String chat(String systemPrompt, String userMessage, boolean jsonFormat) {
        try {
            var messages = List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user",   "content", userMessage)
            );
            var bodyMap = new java.util.LinkedHashMap<String, Object>();
            bodyMap.put("model",       props.getCloudModel());
            bodyMap.put("messages",    messages);
            bodyMap.put("temperature", props.getCloudTemperature());
            bodyMap.put("max_tokens",  jsonFormat ? 6000 : 400);
            if (jsonFormat) bodyMap.put("response_format", Map.of("type", "json_object"));

            String bodyJson = mapper.writeValueAsString(bodyMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(props.getCloudBaseUrl() + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + props.getCloudApiKey())
                    .timeout(Duration.ofSeconds(props.getCloudTimeout()))
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new AiUnavailableException("Proveedor IA HTTP " + response.statusCode() + ": " + response.body());
            }

            CloudChatResponse parsed = mapper.readValue(response.body(), CloudChatResponse.class);
            if (parsed == null || parsed.choices() == null || parsed.choices().isEmpty()
                    || parsed.choices().get(0).message() == null) {
                throw new AiUnavailableException("Respuesta vacía del proveedor IA");
            }
            return parsed.choices().get(0).message().content().trim();

        } catch (AiUnavailableException e) {
            throw e;
        } catch (java.net.http.HttpTimeoutException e) {
            throw new AiUnavailableException("Timeout esperando al proveedor IA");
        } catch (Exception e) {
            log.error("Error llamando al proveedor IA cloud", e);
            throw new AiUnavailableException("Error de comunicación con el proveedor IA: " + e.getMessage());
        }
    }

    @Override
    public boolean ping() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(props.getCloudBaseUrl() + "/models"))
                    .header("Authorization", "Bearer " + props.getCloudApiKey())
                    .timeout(Duration.ofSeconds(8)).GET().build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) { return false; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CloudChatResponse(List<Choice> choices) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(CloudMessage message) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CloudMessage(String role, String content) {}
}