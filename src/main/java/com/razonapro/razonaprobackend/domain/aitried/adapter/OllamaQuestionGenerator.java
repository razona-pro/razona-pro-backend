// src/main/java/com/razonapro/razonaprobackend/domain/aitried/adapter/OllamaQuestionGenerator.java
package com.razonapro.razonaprobackend.domain.aitried.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razonapro.razonaprobackend.domain.aitried.port.AiQuestionGenerator;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiGeneratedQuestion;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiOption;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiQuestionRequest;
import com.razonapro.razonaprobackend.infrastructure.ai.OllamaChatClient;
import com.razonapro.razonaprobackend.infrastructure.ai.OllamaChatClient.OllamaUnavailableException;
import com.razonapro.razonaprobackend.infrastructure.ai.PromptFactory;
import com.razonapro.razonaprobackend.infrastructure.config.AiModelProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.model.provider", havingValue = "OLLAMA")
public class OllamaQuestionGenerator implements AiQuestionGenerator {

    private final OllamaChatClient   client;
    private final PromptFactory      promptFactory;
    private final AiModelProperties  props;
    private final ObjectMapper       mapper;

    @Override
    public AiGeneratedQuestion generate(AiQuestionRequest request) {
        int maxRetries = Math.max(1, props.getOllamaMaxRetries());
        Exception lastError = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("Generando pregunta — competencia={} dificultad={} intento={}/{}",
                        request.competenceName(), request.difficultyLevel(), attempt, maxRetries);

                String raw = client.chat(
                        PromptFactory.QUESTION_SYSTEM,
                        promptFactory.buildQuestionUserPrompt(request),
                        true
                );

                AiGeneratedQuestion question = parseAndValidate(raw, request.difficultyLevel());
                log.info("Pregunta generada exitosamente en intento {}", attempt);
                return question;

            } catch (OllamaUnavailableException e) {
                throw e; // No reintentar si Ollama no está disponible
            } catch (ValidationException e) {
                log.warn("Intento {} fallido por validación: {}", attempt, e.getMessage());
                lastError = e;
            } catch (Exception e) {
                log.warn("Intento {} fallido: {}", attempt, e.getMessage());
                lastError = e;
            }
        }

        throw new OllamaUnavailableException(
                "No se pudo generar una pregunta válida después de " + maxRetries +
                        " intentos. Último error: " + (lastError != null ? lastError.getMessage() : "desconocido"));
    }

    @Override
    public boolean isAvailable() {
        return props.isEnabled()
                && props.getOllamaBaseUrl() != null && !props.getOllamaBaseUrl().isBlank()
                && props.getOllamaModel()   != null && !props.getOllamaModel().isBlank();
    }

    // ── Parser y validador ────────────────────────────────────────────

    private AiGeneratedQuestion parseAndValidate(String raw, int difficultyLevel)
            throws Exception {
        // Limpiar posibles bloques markdown
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            int start = cleaned.indexOf('{');
            int end   = cleaned.lastIndexOf('}');
            if (start >= 0 && end > start) {
                cleaned = cleaned.substring(start, end + 1);
            }
        }

        JsonNode root;
        try {
            root = mapper.readTree(cleaned);
        } catch (Exception e) {
            throw new ValidationException("JSON inválido: " + e.getMessage());
        }

        // Validar statement
        String statement = getTextOrThrow(root, "statement");
        if (statement.length() < 20) {
            throw new ValidationException("Enunciado demasiado corto: " + statement.length() + " chars");
        }

        // Validar opciones
        JsonNode optionsNode = root.get("options");
        if (optionsNode == null || !optionsNode.isArray()) {
            throw new ValidationException("Campo 'options' ausente o no es array");
        }
        if (optionsNode.size() < 3 || optionsNode.size() > 6) {
            throw new ValidationException("Se esperan 3-6 opciones, recibido: " + optionsNode.size());
        }

        List<AiOption> options = new ArrayList<>();
        int correctCount = 0;
        int correctIndex = -1;

        for (int i = 0; i < optionsNode.size(); i++) {
            JsonNode opt = optionsNode.get(i);
            String text = getTextOrThrow(opt, "text");
            if (text.isBlank()) throw new ValidationException("Opción " + i + " tiene texto vacío");

            // Soportar "correct" (bool) o "isCorrect" (bool)
            boolean isCorrect = false;
            if (opt.has("correct"))   isCorrect = opt.get("correct").asBoolean();
            else if (opt.has("isCorrect")) isCorrect = opt.get("isCorrect").asBoolean();

            if (isCorrect) { correctCount++; correctIndex = i; }
            options.add(new AiOption(text, isCorrect));
        }

        if (correctCount != 1) {
            throw new ValidationException("Debe haber exactamente 1 opción correcta, encontradas: " + correctCount);
        }

        String explanation = root.has("explanation")
                ? root.get("explanation").asText("").trim()
                : "";

        return new AiGeneratedQuestion(
                UUID.randomUUID().toString(),
                statement.trim(),
                options,
                correctIndex,
                explanation,
                difficultyLevel
        );
    }

    private String getTextOrThrow(JsonNode node, String field) throws ValidationException {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            throw new ValidationException("Campo obligatorio ausente: " + field);
        }
        return node.get(field).asText("").trim();
    }

    static class ValidationException extends Exception {
        ValidationException(String msg) { super(msg); }
    }
}