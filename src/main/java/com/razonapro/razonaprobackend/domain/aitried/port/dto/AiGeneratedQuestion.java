// src/main/java/com/razonapro/razonaprobackend/domain/aitried/port/dto/AiGeneratedQuestion.java
// (Reemplaza el existente)
package com.razonapro.razonaprobackend.domain.aitried.port.dto;

import java.util.List;

public record AiGeneratedQuestion(
        String       questionId,      // UUID temporal (no BD)
        String       statement,
        List<AiOption> options,       // 4 opciones
        int          correctIndex,    // 0-3
        String       explanation,
        int          difficultyLevel
) {}