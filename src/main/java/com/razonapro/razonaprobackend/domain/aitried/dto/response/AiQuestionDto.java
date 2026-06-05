// domain/aitried/dto/response/AiQuestionDto.java
package com.razonapro.razonaprobackend.domain.aitried.dto.response;

import java.util.List;

public record AiQuestionDto(
        String aiQuestionId,
        String statement,
        List<OptionDto> options,
        int difficultyLevel,
        int questionNumber,
        int totalQuestions,
        int hintsUsed,
        Integer selectedIndex,   // null si no respondida
        Boolean isCorrect,       // null si no respondida
        Integer correctIndex,    // null si no revelada (solo admin/review)
        String explanation       // null si no revelada (solo admin/review)
) {
    public record OptionDto(String id, String text) {}
}