package com.razonapro.razonaprobackend.domain.question.model;

import com.razonapro.razonaprobackend.domain.admin.model.Admin;
import com.razonapro.razonaprobackend.domain.competence.model.Competence;
import com.razonapro.razonaprobackend.shared.ids.QuestionId;
import com.razonapro.razonaprobackend.infrastructure.util.BooleanToYNConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "questions", schema = "razonapro")
@IdClass(QuestionId.class)
public class Question {

    @Id
    @Column(name = "competence_id", length = 6)
    private String competenceId;

    @Id
    @Column(name = "question_id", length = 6)
    private String questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competence_id", insertable = false, updatable = false)
    private Competence competence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Admin admin;

    @Column(name = "statement", length = 300, nullable = false)
    private String statement;

    @Column(name = "explanation", length = 200)
    private String explanation;

    @Column(name = "source", length = 50)
    private String source;

    /** B=Básico, M=Medio, A=Alto */
    @Column(name = "difficulty_level", columnDefinition = "CHAR(1)", nullable = false)
    @Builder.Default
    private String difficultyLevel = "M";

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
