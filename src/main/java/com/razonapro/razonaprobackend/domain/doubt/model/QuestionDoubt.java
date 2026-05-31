// src/main/java/com/razonapro/razonaprobackend/domain/doubt/model/QuestionDoubt.java
package com.razonapro.razonaprobackend.domain.doubt.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "question_doubts")
public class QuestionDoubt {

    @Id
    @Column(name = "doubt_id", length = 12)
    private String doubtId;

    @Column(name = "student_id",  length = 7, nullable = false) private String studentId;
    @Column(name = "program_id",  length = 3, nullable = false) private String programId;
    @Column(name = "source",      length = 10, nullable = false) private String source; // STATIC|AI
    @Column(name = "competence_id",  length = 6)  private String competenceId;
    @Column(name = "question_id",    length = 7)  private String questionId;
    @Column(name = "ai_question_id", length = 12) private String aiQuestionId;

    @Column(name = "statement", columnDefinition = "TEXT") private String statement;
    @Column(name = "message",   length = 500) private String message;

    @Column(name = "status", length = 12, nullable = false)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "reviewed_at") private LocalDateTime reviewedAt;
}