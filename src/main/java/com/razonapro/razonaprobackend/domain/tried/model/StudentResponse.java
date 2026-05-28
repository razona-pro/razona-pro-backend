package com.razonapro.razonaprobackend.domain.tried.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "students_responses")
public class StudentResponse {

    @Id
    @Column(name = "student_response_id", length = 10)
    private String studentResponseId;

    @Column(name = "competence_id", length = 6, nullable = false, updatable = false)
    private String competenceId;

    @Column(name = "test_id", length = 8, nullable = false, updatable = false)
    private String testId;

    @Column(name = "program_id", length = 3, nullable = false, updatable = false)
    private String programId;

    @Column(name = "student_id", length = 7, nullable = false, updatable = false)
    private String studentId;

    @Column(name = "tried_id", length = 10, nullable = false, updatable = false)
    private String triedId;

    @Column(name = "question_id", length = 7, nullable = false, updatable = false)
    private String questionId;

    @Column(name = "option_id", length = 6, nullable = false)
    private String optionId;

    @Column(name = "is_correct", columnDefinition = "CHAR(1)", nullable = false)
    private Boolean isCorrect;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;
}