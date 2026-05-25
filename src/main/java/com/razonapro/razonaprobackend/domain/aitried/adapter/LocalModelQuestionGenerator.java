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
 * Adapter para inferencia local con un modelo ONNX/TensorFlow.
 * Activa cuando ai.model.provider=LOCAL.
 *
 * TODO: cargar modelo desde props.getLocalPath() con OnnxRuntime o TF-Java,
 * implementar tokenización del prompt y postprocesar la salida.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.model.provider", havingValue = "LOCAL")
public class LocalModelQuestionGenerator implements AiQuestionGenerator {

    private final AiModelProperties props;

    @Override
    public AiGeneratedQuestion generate(StudentProfile profile, String competenceId) {
        log.info("Local model generate — path={} student={} competence={}",
                props.getLocalPath(), profile.getStudentId(), competenceId);
        throw new UnsupportedOperationException(
                "LocalModelQuestionGenerator no implementado. Cargar modelo desde " + props.getLocalPath());
    }

    @Override
    public boolean isAvailable() {
        return props.isEnabled()
                && props.getLocalPath() != null
                && !props.getLocalPath().isBlank();
    }
}