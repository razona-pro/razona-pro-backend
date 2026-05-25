package com.razonapro.razonaprobackend.domain.test.model;

import com.razonapro.razonaprobackend.domain.admin.model.Admin;
import com.razonapro.razonaprobackend.domain.competence.model.Competence;
import com.razonapro.razonaprobackend.shared.ids.TestPK;
import com.razonapro.razonaprobackend.infrastructure.util.BooleanToYNConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "tests", schema = "razonapro")
@IdClass(TestPK.class)
public class Test {

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

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @Convert(converter = BooleanToYNConverter.class)
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
}
