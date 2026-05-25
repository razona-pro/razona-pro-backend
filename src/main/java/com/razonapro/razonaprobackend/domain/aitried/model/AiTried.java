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

    /** IN_PROGRESS | FINISHED | ABANDONED */
    @Column(name = "status", length = 15, nullable = false)
    @Builder.Default
    private String status = "IN_PROGRESS";

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
}