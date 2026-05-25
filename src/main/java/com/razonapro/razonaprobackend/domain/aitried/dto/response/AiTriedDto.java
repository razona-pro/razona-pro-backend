package com.razonapro.razonaprobackend.domain.aitried.dto.response;

import com.razonapro.razonaprobackend.domain.aitried.model.AiTried;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Builder
public class AiTriedDto {
    private String aiTriedId;
    private String status;
    private BigDecimal score;
    private Integer totalQuestions;
    private Integer correctAnswers;
    private Integer timeSpentSeconds;
    private String description;
    private LocalDateTime attemptTimestamp;
    private LocalDateTime finishedAt;

    public static AiTriedDto from(AiTried a) {
        return AiTriedDto.builder()
            .aiTriedId(a.getAiTriedId())
            .status(a.getStatus())
            .score(a.getScore())
            .totalQuestions(a.getTotalQuestions())
            .correctAnswers(a.getCorrectAnswers())
            .timeSpentSeconds(a.getTimeSpentSeconds())
            .description(a.getDescription())
            .attemptTimestamp(a.getAttemptTimestamp())
            .finishedAt(a.getFinishedAt())
            .build();
    }
}
