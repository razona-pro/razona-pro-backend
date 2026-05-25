package com.razonapro.razonaprobackend.domain.program.model;

import com.razonapro.razonaprobackend.infrastructure.util.BooleanToYNConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "programs", schema = "razonapro")
public class Program {

    @Id
    @Column(name = "program_id", length = 3)
    private String programId;

    @Column(name = "program_name", length = 20, nullable = false)
    private String programName;

    @Column(name = "description", length = 100)
    private String description;

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
