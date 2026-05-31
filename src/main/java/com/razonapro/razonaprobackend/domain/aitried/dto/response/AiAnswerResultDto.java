// src/main/java/com/razonapro/razonaprobackend/domain/aitried/dto/response/AiAnswerResultDto.java
package com.razonapro.razonaprobackend.domain.aitried.dto.response;

public record AiAnswerResultDto(
        boolean isCorrect,
        String  selectedOptionId,
        String  correctOptionId,
        String  explanation,
        int     correctAnswers,
        int     totalAnswered,
        int     totalQuestions,
        boolean finished,
        Double  finalScore       // presente solo si finished=true
) {}