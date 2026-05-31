// src/main/java/com/razonapro/razonaprobackend/domain/aitried/port/dto/AiGeneratedQuestion.java
package com.razonapro.razonaprobackend.domain.aitried.port.dto;

import java.util.List;

public record AiGeneratedQuestion(
        String         statement,
        List<AiOption> options,
        int            correctIndex,
        String         explanation,
        int            difficultyLevel
) {}