package com.razonapro.razonaprobackend.domain.program.model;

import com.razonapro.razonaprobackend.shared.jpa.Normalizable;
import com.razonapro.razonaprobackend.shared.jpa.NormalizingEntityListener;
import com.razonapro.razonaprobackend.shared.util.StringNormalizer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "programs")
@EntityListeners(NormalizingEntityListener.class)
public class Program implements Normalizable {

    @Id
    @Column(name = "program_id", length = 3)
    private String programId;

    @Column(name = "program_name", length = 50, nullable = false)
    private String programName;

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
        programId   = StringNormalizer.trim(programId);
        programName = StringNormalizer.trim(programName);
        description = StringNormalizer.trim(description);
    }
}