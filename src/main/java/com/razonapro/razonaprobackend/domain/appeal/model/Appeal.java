package com.razonapro.razonaprobackend.domain.appeal.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Apelación de un estudiante con la cuenta desactivada (por plagio o por un admin).
 * Solo puede existir UNA apelación PENDING por estudiante a la vez.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "appeals")
public class Appeal {

    @Id
    @Column(name = "appeal_id", length = 12)
    private String appealId;

    @Column(name = "student_id", length = 7, nullable = false) private String studentId;
    @Column(name = "program_id", length = 3, nullable = false) private String programId;

    /** Motivo de la desactivación al momento de apelar: FRAUD | MANUAL. */
    @Column(name = "deactivation_reason", length = 10) private String deactivationReason;

    @Column(name = "message", length = 1000, nullable = false) private String message;

    /** PENDING | APPROVED | REJECTED */
    @Column(name = "status", length = 10, nullable = false)
    @Builder.Default
    private String status = "PENDING";

    /** Respuesta del admin al resolver (opcional). */
    @Column(name = "admin_response", length = 1000) private String adminResponse;

    /** Admin que resolvió la apelación. */
    @Column(name = "resolved_by", length = 6) private String resolvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at") private LocalDateTime resolvedAt;
}
