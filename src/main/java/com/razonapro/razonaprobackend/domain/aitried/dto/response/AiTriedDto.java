package com.razonapro.razonaprobackend.domain.aitried.dto.response;

import com.razonapro.razonaprobackend.domain.aitried.model.AiTried;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Builder
public class AiTriedDto {
    private String aiTriedId;
    private String studentId;
    private String programId;
    private String competenceId;
    private String status;
    private BigDecimal score;
    private Integer totalQuestions;       // preguntas solicitadas al iniciar
    private Integer correctAnswers;
    private Integer questionsGenerated;   // preguntas realmente generadas por la IA
    private Integer answeredQuestions;    // preguntas respondidas
    private Integer maxPossibleScore;     // puntaje máximo posible (suma de niveles generados)
    private Integer timeSpentSeconds;
    private String description;
    private LocalDateTime attemptTimestamp;
    private LocalDateTime finishedAt;

    public static AiTriedDto from(AiTried a) {
        return AiTriedDto.builder()
                .aiTriedId(a.getAiTriedId())
                .studentId(a.getStudentId())
                .programId(a.getProgramId())
                .competenceId(a.getCompetenceId())
                .status(a.getStatus())
                .score(a.getScore())
                .totalQuestions(a.getTotalQuestions())
                .correctAnswers(a.getCorrectAnswers())
                .questionsGenerated(a.getQuestionsGenerated())
                .answeredQuestions(a.getAnsweredQuestions())
                .maxPossibleScore(a.getMaxPossibleScore())
                .timeSpentSeconds(a.getTimeSpentSeconds())
                .description(a.getDescription())
                .attemptTimestamp(a.getAttemptTimestamp())
                .finishedAt(a.getFinishedAt())
                .build();
    }
}