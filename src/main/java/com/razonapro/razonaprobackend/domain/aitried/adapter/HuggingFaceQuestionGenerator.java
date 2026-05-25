package com.razonapro.razonaprobackend.domain.aitried.adapter;

import com.razonapro.razonaprobackend.domain.aitried.port.AiQuestionGenerator;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiGeneratedQuestion;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.StudentProfile;
import com.razonapro.razonaprobackend.infrastructure.config.AiModelProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adapter para inferencia remota vía Hugging Face Inference API.
 * Activa cuando ai.model.provider=HUGGINGFACE.
 *
 * TODO: implementar llamada HTTP real al endpoint, parseo de respuesta JSON y
 * mapeo a AiGeneratedQuestion. El esqueleto está listo para cuando tengas
 * el modelo fine-tuned subido.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.model.provider", havingValue = "HUGGINGFACE")
public class HuggingFaceQuestionGenerator implements AiQuestionGenerator {

    private final AiModelProperties props;

    @Override
    public AiGeneratedQuestion generate(StudentProfile profile, String competenceId) {
        log.info("HF generate — model={} student={} competence={}",
                props.getHfModelId(), profile.getStudentId(), competenceId);
        throw new UnsupportedOperationException(
                "HuggingFaceQuestionGenerator no implementado. Conectar con " + props.getHfEndpoint());
    }

    @Override
    public boolean isAvailable() {
        return props.isEnabled()
                && props.getHfApiToken() != null
                && !props.getHfApiToken().isBlank()
                && props.getHfModelId() != null
                && !props.getHfModelId().isBlank();
    }
}