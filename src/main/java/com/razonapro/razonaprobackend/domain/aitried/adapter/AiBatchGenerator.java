// src/main/java/com/razonapro/razonaprobackend/domain/aitried/adapter/AiBatchGenerator.java
package com.razonapro.razonaprobackend.domain.aitried.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razonapro.razonaprobackend.domain.aitried.port.AiQuestionGenerator;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiGeneratedQuestion;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiOption;
import com.razonapro.razonaprobackend.infrastructure.ai.AiUnavailableException;
import com.razonapro.razonaprobackend.infrastructure.ai.ChatClient;
import com.razonapro.razonaprobackend.infrastructure.ai.PromptFactory;
import com.razonapro.razonaprobackend.infrastructure.config.AiModelProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiBatchGenerator implements AiQuestionGenerator {

    private final ObjectProvider<ChatClient> chatClientProvider; // null si provider=NONE
    private final PromptFactory     promptFactory;
    private final AiModelProperties props;
    private final ObjectMapper      mapper;

    @Override
    public List<AiGeneratedQuestion> generateBatch(String competenceName, String competenceDescription,
                                                   int totalQuestions, int baseDifficulty) {
        ChatClient client = chatClientProvider.getIfAvailable();
        if (client == null) throw new AiUnavailableException("No hay proveedor IA configurado.");

        int maxRetries = Math.max(1, props.getCloudMaxRetries());
        Exception last = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("Generando batch - competencia={} n={} intento={}/{}",
                        competenceName, totalQuestions, attempt, maxRetries);
                String raw = client.chat(
                        PromptFactory.BATCH_SYSTEM,
                        promptFactory.buildBatchPrompt(competenceName, competenceDescription,
                                totalQuestions, baseDifficulty),
                        true);
                List<AiGeneratedQuestion> qs = parseBatch(raw, totalQuestions);
                if (qs.isEmpty()) throw new ValidationException("Batch vacío");
                log.info("Batch generado: {} preguntas en intento {}", qs.size(), attempt);
                return qs;
            } catch (AiUnavailableException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Intento {} fallido: {}", attempt, e.getMessage());
                last = e;
            }
        }
        throw new AiUnavailableException("No se pudo generar el batch tras " + maxRetries +
                " intentos. " + (last != null ? last.getMessage() : ""));
    }

    @Override
    public boolean isAvailable() {
        ChatClient c = chatClientProvider.getIfAvailable();
        return props.isEnabled() && c != null && c.ping();
    }

    // ── Parser ──
    private List<AiGeneratedQuestion> parseBatch(String raw, int expected) throws Exception {
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            int s = cleaned.indexOf('{'), e = cleaned.lastIndexOf('}');
            if (s >= 0 && e > s) cleaned = cleaned.substring(s, e + 1);
        }
        JsonNode root = mapper.readTree(cleaned);
        JsonNode arr = root.get("questions");
        if (arr == null || !arr.isArray() || arr.isEmpty())
            throw new ValidationException("Falta array 'questions'");

        List<AiGeneratedQuestion> result = new ArrayList<>();
        for (JsonNode qn : arr) {
            try {
                result.add(parseOne(qn));
            } catch (ValidationException ve) {
                log.warn("Pregunta descartada: {}", ve.getMessage());
            }
        }
        if (result.isEmpty()) throw new ValidationException("Ninguna pregunta válida en el batch");
        return result;
    }

    private AiGeneratedQuestion parseOne(JsonNode qn) throws ValidationException {
        String statement = text(qn, "statement");
        if (statement.length() < 20) throw new ValidationException("Enunciado corto");

        JsonNode opts = qn.get("options");
        if (opts == null || !opts.isArray() || opts.size() < 3 || opts.size() > 6)
            throw new ValidationException("Opciones inválidas");

        List<AiOption> options = new ArrayList<>();
        int correctCount = 0, correctIdx = -1;
        for (int i = 0; i < opts.size(); i++) {
            JsonNode o = opts.get(i);
            String t = text(o, "text");
            boolean correct = o.has("correct") ? o.get("correct").asBoolean()
                    : o.has("isCorrect") && o.get("isCorrect").asBoolean();
            if (correct) { correctCount++; correctIdx = i; }
            options.add(new AiOption(t, correct));
        }
        if (correctCount != 1) throw new ValidationException("Debe haber 1 correcta, hay " + correctCount);

        String expl = qn.has("explanation") ? qn.get("explanation").asText("").trim() : "";
        int diff = qn.has("difficulty") ? clamp(qn.get("difficulty").asInt(5), 1, 10) : 5;

        return new AiGeneratedQuestion(statement.trim(), options, correctIdx, expl, diff);
    }

    private String text(JsonNode n, String f) throws ValidationException {
        if (n == null || !n.has(f) || n.get(f).isNull())
            throw new ValidationException("Campo ausente: " + f);
        String v = n.get(f).asText("").trim();
        if (v.isBlank()) throw new ValidationException("Campo vacío: " + f);
        return v;
    }

    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    static class ValidationException extends Exception { ValidationException(String m) { super(m); } }
}