// src/main/java/com/razonapro/razonaprobackend/domain/aitried/dto/response/AiQuestionDto.java
package com.razonapro.razonaprobackend.domain.aitried.dto.response;

import java.util.List;

public record AiQuestionDto(
        String        questionId,
        String        statement,
        List<OptionDto> options,
        int           difficultyLevel,
        int           questionNumber,
        int           totalQuestions
) {
    public record OptionDto(String id, String text) {}
}