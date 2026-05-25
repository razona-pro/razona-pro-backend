package com.razonapro.razonaprobackend.domain.test.model;

import com.razonapro.razonaprobackend.domain.admin.model.Admin;
import com.razonapro.razonaprobackend.domain.question.model.Question;
import com.razonapro.razonaprobackend.infrastructure.util.BooleanToYNConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "tests_questions", schema = "razonapro")
public class TestQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
                    generator = "seq_test_question")
    @SequenceGenerator(name = "seq_test_question",
                       sequenceName = "razonapro.seq_test_question_id",
                       allocationSize = 1)
    @Column(name = "test_question_id")
    private Integer testQuestionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Admin admin;

    @Column(name = "competence_id", length = 6, nullable = false)
    private String competenceId;

    @Column(name = "test_id", length = 8, nullable = false)
    private String testId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "test_id",       referencedColumnName = "test_id",       insertable = false, updatable = false),
        @JoinColumn(name = "competence_id", referencedColumnName = "competence_id", insertable = false, updatable = false)
    })
    private Test test;

    @Column(name = "question_id", length = 6, nullable = false)
    private String questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "competence_id", referencedColumnName = "competence_id", insertable = false, updatable = false),
        @JoinColumn(name = "question_id",   referencedColumnName = "question_id",   insertable = false, updatable = false)
    })
    private Question question;

    @Column(name = "question_order")
    private Integer questionOrder;

    @Convert(converter = BooleanToYNConverter.class)
    @Column(name = "is_active", columnDefinition = "CHAR(1)", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
