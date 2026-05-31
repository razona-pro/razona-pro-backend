// src/main/java/com/razonapro/razonaprobackend/domain/aitried/adapter/LocalModelQuestionGenerator.java
package com.razonapro.razonaprobackend.domain.aitried.adapter;

import com.razonapro.razonaprobackend.domain.aitried.port.AiQuestionGenerator;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiGeneratedQuestion;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiQuestionRequest;
import com.razonapro.razonaprobackend.infrastructure.config.AiModelProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.model.provider", havingValue = "LOCAL")
public class LocalModelQuestionGenerator implements AiQuestionGenerator {

    private final AiModelProperties props;

    @Override
    public AiGeneratedQuestion generate(AiQuestionRequest request) {
        throw new UnsupportedOperationException(
                "LocalModelQuestionGenerator no implementado. Usa OLLAMA.");
    }

    @Override
    public boolean isAvailable() {
        return props.isEnabled() && props.getLocalPath() != null && !props.getLocalPath().isBlank();
    }
}