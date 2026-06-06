package com.razonapro.razonaprobackend.domain.tried.dto.response;

/**
 * Indica si un estudiante puede entrar a una prueba.
 *
 * @param allowed            true si puede entrar (iniciar un intento nuevo o reanudar uno activo)
 * @param reason             motivo cuando no se permite entrar (null si allowed = true)
 * @param testActive         si la prueba está activa
 * @param hasQuestions       si la prueba tiene preguntas activas
 * @param inProgressTriedId  si != null, hay un intento activo que debe REANUDARSE en vez de iniciar uno nuevo
 */
public record TriedEligibilityDto(
        boolean allowed,
        String  reason,
        boolean testActive,
        boolean hasQuestions,
        String  inProgressTriedId
) {}
