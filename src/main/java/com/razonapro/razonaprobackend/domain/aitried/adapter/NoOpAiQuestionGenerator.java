package com.razonapro.razonaprobackend.domain.aitried.adapter;

import com.razonapro.razonaprobackend.domain.aitried.port.AiQuestionGenerator;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiGeneratedQuestion;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.StudentProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Implementación por defecto: deshabilitada.
 * Activa cuando ai.model.provider=NONE (o no está definida).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.model.provider", havingValue = "NONE", matchIfMissing = true)
public class NoOpAiQuestionGenerator implements AiQuestionGenerator {

    @Override
    public AiGeneratedQuestion generate(StudentProfile profile, String competenceId) {
        log.warn("AiQuestionGenerator deshabilitado (ai.model.provider=NONE)");
        return null;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}