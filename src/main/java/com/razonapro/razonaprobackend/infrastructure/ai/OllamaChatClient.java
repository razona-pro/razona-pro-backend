// src/main/java/com/razonapro/razonaprobackend/infrastructure/ai/OllamaChatClient.java
package com.razonapro.razonaprobackend.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@ConditionalOnProperty(name = "ai.model.provider", havingValue = "OLLAMA")
public class OllamaChatClient {

    private final AiModelProperties props;
    private final ObjectMapper       mapper;
    private final HttpClient         httpClient;

    public OllamaChatClient(AiModelProperties props, ObjectMapper mapper) {
        this.props  = props;
        this.mapper = mapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Llama al endpoint /api/chat de Ollama.
     * @param systemPrompt Instrucciones del sistema
     * @param userMessage  Mensaje del usuario
     * @param jsonFormat   Si es true, solicita salida JSON estricta
     * @return Texto generado por el modelo
     */
    public String chat(String systemPrompt, String userMessage, boolean jsonFormat) {
        try {
            var messages = List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user",   "content", userMessage)
            );

            var bodyMap = new java.util.LinkedHashMap<String, Object>();
            bodyMap.put("model",    props.getOllamaModel());
            bodyMap.put("messages", messages);
            bodyMap.put("stream",   false);
            bodyMap.put("options",  Map.of(
                    "temperature", props.getOllamaTemperature(),
                    "num_predict", jsonFormat ? 1200 : 350
            ));
            if (jsonFormat) {
                bodyMap.put("format", "json");
            }

            String bodyJson = mapper.writeValueAsString(bodyMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(props.getOllamaBaseUrl() + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(props.getOllamaTimeout()))
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new OllamaUnavailableException(
                        "Ollama respondió con HTTP " + response.statusCode());
            }

            OllamaChatResponse chatResponse = mapper.readValue(
                    response.body(), OllamaChatResponse.class);

            if (chatResponse == null || chatResponse.message() == null) {
                throw new OllamaUnavailableException("Respuesta vacía de Ollama");
            }

            return chatResponse.message().content().trim();

        } catch (OllamaUnavailableException e) {
            throw e;
        } catch (java.net.ConnectException e) {
            throw new OllamaUnavailableException(
                    "No se pudo conectar a Ollama en " + props.getOllamaBaseUrl() +
                            ". Asegúrate de que Ollama esté corriendo: `ollama serve`");
        } catch (Exception e) {
            log.error("Error llamando a Ollama", e);
            throw new OllamaUnavailableException("Error de comunicación con Ollama: " + e.getMessage());
        }
    }

    /** Verifica si Ollama está accesible. */
    public boolean ping() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(props.getOllamaBaseUrl() + "/api/tags"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Records internos ──────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OllamaChatResponse(
            OllamaMessage message,
            boolean done
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OllamaMessage(
            String role,
            String content
    ) {}

    /** Excepción técnica que se traduce a API error en la capa de servicio */
    public static class OllamaUnavailableException extends RuntimeException {
        public OllamaUnavailableException(String msg) { super(msg); }
    }
}