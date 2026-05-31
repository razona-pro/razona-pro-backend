// domain/doubt/dto/DoubtDto.java
package com.razonapro.razonaprobackend.domain.doubt.dto;

import com.razonapro.razonaprobackend.domain.doubt.model.QuestionDoubt;
import java.time.LocalDateTime;

public record DoubtDto(
        String doubtId, String studentId, String source, String competenceId,
        String questionId, String aiQuestionId, String statement, String message,
        String status, LocalDateTime createdAt) {
    public static DoubtDto from(QuestionDoubt d) {
        return new DoubtDto(d.getDoubtId(), d.getStudentId(), d.getSource(), d.getCompetenceId(),
                d.getQuestionId(), d.getAiQuestionId(), d.getStatement(), d.getMessage(),
                d.getStatus(), d.getCreatedAt());
    }
}