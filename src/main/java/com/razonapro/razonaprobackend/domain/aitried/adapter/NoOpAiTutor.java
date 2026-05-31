// src/main/java/com/razonapro/razonaprobackend/domain/aitried/adapter/NoOpAiTutor.java
package com.razonapro.razonaprobackend.domain.aitried.adapter;

import com.razonapro.razonaprobackend.domain.aitried.port.AiTutor;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiHintContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnMissingBean(value = AiTutor.class, ignored = NoOpAiTutor.class)
public class NoOpAiTutor implements AiTutor {

    @Override
    public String generateHint(AiHintContext context) {
        return null;
    }

    @Override
    public boolean isAvailable() { return false; }
}