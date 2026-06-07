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
    /** Competencias de la sesión (multi-competencia); incluye al menos la principal. */
    private java.util.List<String> competenceIds;
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
                .competenceIds(parseComps(a))
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

    /** Competencias de la sesión desde el CSV (cae a la principal si está vacío). */
    private static java.util.List<String> parseComps(AiTried a) {
        if (a.getCompetenceIdsCsv() != null && !a.getCompetenceIdsCsv().isBlank()) {
            java.util.List<String> out = new java.util.ArrayList<>();
            for (String s : a.getCompetenceIdsCsv().split(",")) {
                String c = s.trim();
                if (!c.isEmpty()) out.add(c);
            }
            if (!out.isEmpty()) return out;
        }
        return a.getCompetenceId() != null ? java.util.List.of(a.getCompetenceId()) : java.util.List.of();
    }
}