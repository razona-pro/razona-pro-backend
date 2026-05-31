// src/main/java/com/razonapro/razonaprobackend/domain/aitried/adapter/NoOpAiQuestionGenerator.java
package com.razonapro.razonaprobackend.domain.aitried.adapter;

import com.razonapro.razonaprobackend.domain.aitried.port.AiQuestionGenerator;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiGeneratedQuestion;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiQuestionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "ai.model.provider", havingValue = "NONE", matchIfMissing = true)
public class NoOpAiQuestionGenerator implements AiQuestionGenerator {

    @Override
    public AiGeneratedQuestion generate(AiQuestionRequest request) {
        log.warn("AiQuestionGenerator deshabilitado (NONE). Configura OLLAMA para activarlo.");
        return null;
    }

    @Override
    public boolean isAvailable() { return false; }
}