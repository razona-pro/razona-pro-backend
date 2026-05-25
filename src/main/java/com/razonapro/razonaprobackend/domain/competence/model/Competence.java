package com.razonapro.razonaprobackend.domain.competence.model;

import com.razonapro.razonaprobackend.shared.jpa.Normalizable;
import com.razonapro.razonaprobackend.shared.jpa.NormalizingEntityListener;
import com.razonapro.razonaprobackend.shared.util.StringNormalizer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "competences")
@EntityListeners(NormalizingEntityListener.class)
public class Competence implements Normalizable {

    @Id
    @Column(name = "competence_id", length = 6)
    private String competenceId;

    @Column(name = "competence_name", length = 30, nullable = false)
    private String competenceName;

    @Column(name = "description", length = 100)
    private String description;

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

    @Override
    public void normalize() {
        competenceId   = StringNormalizer.upper(competenceId);
        competenceName = StringNormalizer.upper(competenceName);
        description    = StringNormalizer.upper(description);
    }
}