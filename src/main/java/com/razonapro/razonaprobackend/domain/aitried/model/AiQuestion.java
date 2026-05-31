// src/main/java/com/razonapro/razonaprobackend/domain/aitried/model/AiQuestion.java
package com.razonapro.razonaprobackend.domain.aitried.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "ai_questions")
public class AiQuestion {

    @Id
    @Column(name = "ai_question_id", length = 12)
    private String aiQuestionId;

    @Column(name = "program_id",  length = 3,  nullable = false) private String programId;
    @Column(name = "student_id",  length = 7,  nullable = false) private String studentId;
    @Column(name = "ai_tried_id", length = 10, nullable = false) private String aiTriedId;
    @Column(name = "competence_id", length = 6) private String competenceId;

    @Column(name = "question_order", nullable = false) private Integer questionOrder;

    @Column(name = "statement", columnDefinition = "TEXT", nullable = false)
    private String statement;

    @Column(name = "options_json", columnDefinition = "TEXT", nullable = false)
    private String optionsJson;

    @Column(name = "correct_index", nullable = false) private Integer correctIndex;

    @Column(name = "explanation", columnDefinition = "TEXT") private String explanation;

    @Column(name = "difficulty_level", nullable = false)
    @Builder.Default
    private Integer difficultyLevel = 5;

    @Column(name = "selected_index") private Integer selectedIndex;

    @Column(name = "is_correct", columnDefinition = "CHAR(1)")
    private Boolean isCorrect;

    @Column(name = "hints_used", nullable = false)
    @Builder.Default
    private Integer hintsUsed = 0;

    @Column(name = "answered_at") private LocalDateTime answeredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}