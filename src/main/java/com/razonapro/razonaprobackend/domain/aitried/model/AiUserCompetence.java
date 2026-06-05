package com.razonapro.razonaprobackend.domain.aitried.model;

import com.razonapro.razonaprobackend.shared.ids.AiUserCompetenceId;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * IRT acumulativo por usuario y competencia. El theta persiste ENTRE intentos IA:
 * un nuevo intento arranca en el nivel acumulado del usuario, no siempre en el medio.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "ai_user_competence")
@IdClass(AiUserCompetenceId.class)
public class AiUserCompetence {

    @Id @Column(name = "program_id",    length = 3) private String programId;
    @Id @Column(name = "student_id",    length = 7) private String studentId;
    @Id @Column(name = "competence_id", length = 6) private String competenceId;

    @Column(name = "theta", precision = 5, scale = 3, nullable = false)
    @Builder.Default
    private BigDecimal theta = BigDecimal.ZERO;

    @Column(name = "answered_total", nullable = false)
    @Builder.Default
    private Integer answeredTotal = 0;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
