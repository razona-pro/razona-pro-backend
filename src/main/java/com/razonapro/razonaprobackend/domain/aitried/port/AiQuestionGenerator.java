// src/main/java/com/razonapro/razonaprobackend/domain/aitried/port/AiQuestionGenerator.java
package com.razonapro.razonaprobackend.domain.aitried.port;

import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiGeneratedQuestion;
import java.util.List;

public interface AiQuestionGenerator {
    /** Genera todas las preguntas del intento en una sola llamada. */
    List<AiGeneratedQuestion> generateBatch(String competenceName, String competenceDescription,
                                            int totalQuestions, int baseDifficulty);
    boolean isAvailable();
}