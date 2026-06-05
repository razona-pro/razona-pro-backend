package com.razonapro.razonaprobackend.domain.tried.model;

import com.razonapro.razonaprobackend.domain.student.model.Student;
import com.razonapro.razonaprobackend.domain.test.model.Test;
import com.razonapro.razonaprobackend.shared.ids.TriedId;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "trieds")
@IdClass(TriedId.class)
public class Tried {

    @Id @Column(name = "competence_id", length = 6) private String competenceId;
    @Id @Column(name = "test_id",       length = 8) private String testId;
    @Id @Column(name = "program_id",    length = 3) private String programId;
    @Id @Column(name = "student_id",    length = 7) private String studentId;
    @Id @Column(name = "tried_id",      length = 10) private String triedId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "student_id", referencedColumnName = "student_id", insertable = false, updatable = false),
            @JoinColumn(name = "program_id", referencedColumnName = "program_id", insertable = false, updatable = false)
    })
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "test_id",       referencedColumnName = "test_id",       insertable = false, updatable = false),
            @JoinColumn(name = "competence_id", referencedColumnName = "competence_id", insertable = false, updatable = false)
    })
    private Test test;

    /** IN_PROGRESS | FINISHED | ABANDONED | TIMED_OUT | ANULADO (anulado por fraude) */
    @Column(name = "status", length = 15, nullable = false)
    @Builder.Default
    private String status = "IN_PROGRESS";

    /** Nº de eventos sospechosos detectados (cambios de pestaña, etc.). */
    @Column(name = "fraud_attempts", nullable = false)
    @Builder.Default
    private Integer fraudAttempts = 0;

    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(name = "correct_answers")
    @Builder.Default
    private Integer correctAnswers = 0;

    @Column(name = "attempt_timestamp", nullable = false)
    @Builder.Default
    private LocalDateTime attemptTimestamp = LocalDateTime.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}