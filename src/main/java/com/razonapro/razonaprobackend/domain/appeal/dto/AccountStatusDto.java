package com.razonapro.razonaprobackend.domain.appeal.dto;

import lombok.Builder;
import lombok.Getter;

/** Estado de la cuenta del estudiante para el flujo de apelación en el login. */
@Getter @Builder
public class AccountStatusDto {
    /** true si la cuenta está activa (no necesita apelar). */
    private boolean active;
    /** Motivo de la desactivación: FRAUD | MANUAL | null. */
    private String  deactivationReason;
    /** true si ya hay una apelación PENDING (no puede enviar otra). */
    private boolean hasPendingAppeal;
    /** Estado de la última apelación (PENDING|APPROVED|REJECTED) o null si nunca apeló. */
    private String  lastAppealStatus;
    /** Respuesta del admin a la última apelación resuelta (si la hay). */
    private String  lastAdminResponse;
}
