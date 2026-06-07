package com.razonapro.razonaprobackend.domain.aitried.model;

import com.razonapro.razonaprobackend.domain.student.model.Student;
import com.razonapro.razonaprobackend.shared.ids.AiTriedId;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "ai_trieds")
@IdClass(AiTriedId.class)
public class AiTried {

    @Id @Column(name = "program_id",  length = 3)  private String programId;
    @Id @Column(name = "student_id",  length = 7)  private String studentId;
    @Id @Column(name = "ai_tried_id", length = 10) private String aiTriedId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "student_id", referencedColumnName = "student_id", insertable = false, updatable = false),
            @JoinColumn(name = "program_id", referencedColumnName = "program_id", insertable = false, updatable = false)
    })
    private Student student;

    /** IN_PROGRESS | FINISHED | ABANDONED | ANULADO (anulado por fraude) */
    @Column(name = "status", length = 15, nullable = false)
    @Builder.Default
    private String status = "IN_PROGRESS";

    /** Nº de eventos sospechosos detectados (cambios de pestaña, etc.). */
    @Column(name = "fraud_attempts", nullable = false)
    @Builder.Default
    private Integer fraudAttempts = 0;

    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(name = "correct_answers")
    @Builder.Default
    private Integer correctAnswers = 0;

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "attempt_timestamp", nullable = false)
    @Builder.Default
    private LocalDateTime attemptTimestamp = LocalDateTime.now();

    @Column(name = "description", length = 200)
    private String description;

    /** Competencia principal (la primera elegida). Se conserva por compatibilidad y para mostrar. */
    @Column(name = "competence_id", length = 6)
    private String competenceId;

    /**
     * Competencias de la sesión separadas por coma (multi-competencia). La sesión va rotando
     * entre ellas al generar cada pregunta. Si es null, se usa solo competenceId.
     */
    @Column(name = "competence_ids", length = 80)
    private String competenceIdsCsv;

    @Column(name = "theta", precision = 5, scale = 3)
    @Builder.Default
    private java.math.BigDecimal theta = java.math.BigDecimal.ZERO;

    /** Nº de preguntas que la IA generó en la sesión (puede ser menor a total si se abandona). */
    @Column(name = "questions_generated", nullable = false)
    @Builder.Default
    private Integer questionsGenerated = 0;

    /** Nº de preguntas respondidas por el estudiante. */
    @Column(name = "answered_questions", nullable = false)
    @Builder.Default
    private Integer answeredQuestions = 0;

    /** Suma de los niveles de las preguntas generadas = puntaje máximo posible. */
    @Column(name = "max_possible_score", nullable = false)
    @Builder.Default
    private Integer maxPossibleScore = 0;
}