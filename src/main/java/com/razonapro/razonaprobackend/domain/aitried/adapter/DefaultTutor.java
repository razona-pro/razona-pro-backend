// src/main/java/com/razonapro/razonaprobackend/domain/aitried/adapter/DefaultTutor.java
package com.razonapro.razonaprobackend.domain.aitried.adapter;

import com.razonapro.razonaprobackend.domain.aitried.port.AiTutor;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiHintContext;
import com.razonapro.razonaprobackend.infrastructure.ai.AiUnavailableException;
import com.razonapro.razonaprobackend.infrastructure.ai.ChatClient;
import com.razonapro.razonaprobackend.infrastructure.ai.PromptFactory;
import com.razonapro.razonaprobackend.infrastructure.config.AiModelProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultTutor implements AiTutor {

    private final ObjectProvider<ChatClient> chatClientProvider;
    private final PromptFactory     promptFactory;
    private final AiModelProperties props;

    @Override
    public String generateHint(AiHintContext ctx) {
        ChatClient client = chatClientProvider.getIfAvailable();
        if (client == null) throw new AiUnavailableException("No hay proveedor IA configurado.");
        return client.chat(PromptFactory.HINT_SYSTEM, promptFactory.buildHintUserPrompt(ctx), false);
    }

    @Override
    public boolean isAvailable() {
        ChatClient c = chatClientProvider.getIfAvailable();
        return props.isEnabled() && c != null;
    }
}