package com.razonapro.razonaprobackend.domain.question.dto.response;

import com.razonapro.razonaprobackend.domain.question.model.Option;
import com.razonapro.razonaprobackend.domain.question.model.Question;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter @Builder
public class QuestionDto {
    private String competenceId;
    private String questionId;
    private String statement;
    private String explanation;
    private String source;
    private String difficultyLevel;
    private Boolean isActive;
    private List<OptionDto> options;

    public static QuestionDto from(Question q) {
        return QuestionDto.builder()
            .competenceId(q.getCompetenceId())
            .questionId(q.getQuestionId())
            .statement(q.getStatement())
            .explanation(q.getExplanation())
            .source(q.getSource())
            .difficultyLevel(q.getDifficultyLevel())
            .isActive(q.getIsActive())
            .build();
    }

    public static QuestionDto from(Question q, List<Option> opts) {
        QuestionDto dto = from(q);
        return QuestionDto.builder()
            .competenceId(dto.competenceId)
            .questionId(dto.questionId)
            .statement(dto.statement)
            .explanation(dto.explanation)
            .source(dto.source)
            .difficultyLevel(dto.difficultyLevel)
            .isActive(dto.isActive)
            .options(opts.stream().map(OptionDto::from).toList())
            .build();
    }
}
