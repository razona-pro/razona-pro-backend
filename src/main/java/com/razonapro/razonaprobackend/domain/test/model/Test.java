package com.razonapro.razonaprobackend.domain.test.model;

import com.razonapro.razonaprobackend.domain.admin.model.Admin;
import com.razonapro.razonaprobackend.domain.competence.model.Competence;
import com.razonapro.razonaprobackend.shared.ids.TestPK;
import com.razonapro.razonaprobackend.shared.jpa.Normalizable;
import com.razonapro.razonaprobackend.shared.jpa.NormalizingEntityListener;
import com.razonapro.razonaprobackend.shared.util.StringNormalizer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "tests")
@IdClass(TestPK.class)
@EntityListeners(NormalizingEntityListener.class)
public class Test implements Normalizable {

    @Id
    @Column(name = "test_id", length = 8)
    private String testId;

    @Id
    @Column(name = "competence_id", length = 6)
    private String competenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competence_id", insertable = false, updatable = false)
    private Competence competence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Admin admin;

    @Column(name = "test_name", length = 50, nullable = false)
    private String testName;

    @Column(name = "description", length = 100)
    private String description;

    /** NULL = sin tiempo (solo permitido en modo PRACTICE). */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "is_active", columnDefinition = "CHAR(1)", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "questions_to_present")
    private Integer questionsToPresent;

    /** PRACTICE | EXAM | TIMED */
    @Column(name = "test_mode", length = 10, nullable = false)
    @Builder.Default
    private String testMode = "PRACTICE";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    @Override
    public void normalize() {
        testName    = StringNormalizer.trim(testName);
        description = StringNormalizer.trim(description);
    }
}