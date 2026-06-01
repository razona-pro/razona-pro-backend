// domain/doubt/dto/DoubtDto.java
package com.razonapro.razonaprobackend.domain.doubt.dto;

import com.razonapro.razonaprobackend.domain.doubt.model.QuestionDoubt;
import com.razonapro.razonaprobackend.domain.question.dto.response.OptionDto;
import java.time.LocalDateTime;
import java.util.List;

public record DoubtDto(
        String doubtId, String studentId, String source, String competenceId,
        String questionId, String aiQuestionId, String statement, String message,
        String status, LocalDateTime createdAt, List<OptionDto> options) {

    public static DoubtDto from(QuestionDoubt d) {
        return new DoubtDto(d.getDoubtId(), d.getStudentId(), d.getSource(), d.getCompetenceId(),
                d.getQuestionId(), d.getAiQuestionId(), d.getStatement(), d.getMessage(),
                d.getStatus(), d.getCreatedAt(), null);
    }

    public static DoubtDto from(QuestionDoubt d, List<OptionDto> options) {
        return new DoubtDto(d.getDoubtId(), d.getStudentId(), d.getSource(), d.getCompetenceId(),
                d.getQuestionId(), d.getAiQuestionId(), d.getStatement(), d.getMessage(),
                d.getStatus(), d.getCreatedAt(), options);
    }
}
