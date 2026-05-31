// src/main/java/com/razonapro/razonaprobackend/domain/aitried/port/dto/AiQuestionRequest.java
package com.razonapro.razonaprobackend.domain.aitried.port.dto;

import java.util.List;

public record AiQuestionRequest(
        String competenceId,
        String competenceName,
        String competenceDescription,
        int    difficultyLevel,          // 1-10
        List<String> statementsToAvoid   // para no repetir
) {}