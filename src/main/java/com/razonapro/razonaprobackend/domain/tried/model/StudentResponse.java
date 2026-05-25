package com.razonapro.razonaprobackend.domain.tried.model;

import com.razonapro.razonaprobackend.domain.question.model.Option;
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

    @Column(name = "competence_id", length = 6, nullable = false) private String competenceId;
    @Column(name = "test_id",       length = 8, nullable = false) private String testId;
    @Column(name = "program_id",    length = 3, nullable = false) private String programId;
    @Column(name = "student_id",    length = 7, nullable = false) private String studentId;
    @Column(name = "tried_id",      length = 10, nullable = false) private String triedId;
    @Column(name = "question_id",   length = 7, nullable = false) private String questionId;
    @Column(name = "option_id",     length = 6, nullable = false) private String optionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "competence_id", referencedColumnName = "competence_id", insertable = false, updatable = false),
            @JoinColumn(name = "test_id",       referencedColumnName = "test_id",       insertable = false, updatable = false),
            @JoinColumn(name = "program_id",    referencedColumnName = "program_id",    insertable = false, updatable = false),
            @JoinColumn(name = "student_id",    referencedColumnName = "student_id",    insertable = false, updatable = false),
            @JoinColumn(name = "tried_id",      referencedColumnName = "tried_id",      insertable = false, updatable = false)
    })
    private Tried tried;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "competence_id", referencedColumnName = "competence_id", insertable = false, updatable = false),
            @JoinColumn(name = "question_id",   referencedColumnName = "question_id",   insertable = false, updatable = false),
            @JoinColumn(name = "option_id",     referencedColumnName = "option_id",     insertable = false, updatable = false)
    })
    private Option option;

    @Column(name = "is_correct", columnDefinition = "CHAR(1)", nullable = false)
    private Boolean isCorrect;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;
}