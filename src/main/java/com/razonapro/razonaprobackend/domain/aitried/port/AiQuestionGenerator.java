package com.razonapro.razonaprobackend.domain.aitried.port;

import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiGeneratedQuestion;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.StudentProfile;

/**
 * Puerto (hexagonal) para generación de preguntas mediante modelos de IA.
 * Implementaciones intercambiables: NoOp, HuggingFace, Local (ONNX/TensorFlow).
 */
public interface AiQuestionGenerator {

    /** Genera una pregunta personalizada para el perfil del estudiante. */
    AiGeneratedQuestion generate(StudentProfile profile, String competenceId);

    /** Indica si el generador está operativo y configurado. */
    boolean isAvailable();
}