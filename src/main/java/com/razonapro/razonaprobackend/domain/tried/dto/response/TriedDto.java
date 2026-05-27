package com.razonapro.razonaprobackend.domain.tried.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.razonapro.razonaprobackend.domain.test.model.TestQuestion;
import com.razonapro.razonaprobackend.domain.tried.model.Tried;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TriedDto {

    private String        triedId;
    private String        testId;
    private String        testName;
    private String        competenceId;
    private String        status;
    private BigDecimal    score;
    private Integer       totalQuestions;
    private Integer       correctAnswers;
    private Integer       timeSpentSeconds;
    private LocalDateTime attemptTimestamp;
    private LocalDateTime finishedAt;
    private List<String>  questionIds;

    public static TriedDto from(Tried t) {
        return TriedDto.builder()
                .triedId(t.getTriedId())
                .testId(t.getTestId())
                .testName(t.getTest() != null ? t.getTest().getTestName() : null)
                .competenceId(t.getCompetenceId())
                .status(t.getStatus())
                .score(t.getScore())
                .totalQuestions(t.getTotalQuestions())
                .correctAnswers(t.getCorrectAnswers())
                .timeSpentSeconds(t.getTimeSpentSeconds())
                .attemptTimestamp(t.getAttemptTimestamp())
                .finishedAt(t.getFinishedAt())
                .build();
    }

    public static TriedDto fromWithQuestions(Tried t, List<TestQuestion> questions) {
        return TriedDto.builder()
                .triedId(t.getTriedId())
                .testId(t.getTestId())
                .competenceId(t.getCompetenceId())
                .status(t.getStatus())
                .totalQuestions(t.getTotalQuestions())
                .attemptTimestamp(t.getAttemptTimestamp())
                .questionIds(questions.stream().map(TestQuestion::getQuestionId).toList())
                .build();
    }
}