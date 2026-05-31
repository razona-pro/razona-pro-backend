// domain/aitried/dto/response/AiAnswerResultDto.java
package com.razonapro.razonaprobackend.domain.aitried.dto.response;

public record AiAnswerResultDto(
        boolean isCorrect,
        int     selectedIndex,
        int     correctIndex,
        String  explanation,
        int     correctAnswers,
        int     totalAnswered,
        int     totalQuestions,
        boolean finished,
        Double  finalScore
) {}