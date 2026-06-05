package com.razonapro.razonaprobackend.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Respuesta del reenvío: segundos que el cliente debe esperar antes de poder reenviar otra vez. */
@Getter
@AllArgsConstructor
public class ResendVerificationResponse {
    private int cooldownSeconds;
}
