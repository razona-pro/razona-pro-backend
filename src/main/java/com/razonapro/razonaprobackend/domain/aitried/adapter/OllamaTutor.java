// src/main/java/com/razonapro/razonaprobackend/domain/aitried/adapter/OllamaTutor.java
package com.razonapro.razonaprobackend.domain.aitried.adapter;

import com.razonapro.razonaprobackend.domain.aitried.port.AiTutor;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiHintContext;
import com.razonapro.razonaprobackend.infrastructure.ai.OllamaChatClient;
import com.razonapro.razonaprobackend.infrastructure.ai.PromptFactory;
import com.razonapro.razonaprobackend.infrastructure.config.AiModelProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.model.provider", havingValue = "OLLAMA")
public class OllamaTutor implements AiTutor {

    private final OllamaChatClient  client;
    private final PromptFactory     promptFactory;
    private final AiModelProperties props;

    @Override
    public String generateHint(AiHintContext context) {
        log.info("Generando pista nivel={} competencia={}",
                context.hintLevel(), context.competenceName());
        return client.chat(
                PromptFactory.HINT_SYSTEM,
                promptFactory.buildHintUserPrompt(context),
                false
        );
    }

    @Override
    public boolean isAvailable() { return props.isEnabled(); }
}