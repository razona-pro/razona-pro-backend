package com.razonapro.razonaprobackend.domain.tried.dto.response;

import com.razonapro.razonaprobackend.domain.tried.model.Tried;
import lombok.Builder;
import lombok.Getter;

/**
 * Estado de reanudación de un intento. El tiempo es autoritativo del servidor:
 * se calcula desde attempt_timestamp + duración del test, no desde el cliente.
 */
@Getter
@Builder
public class TriedResumeDto {

    private TriedDto tried;
    /** Segundos restantes. null = sin tiempo (práctica). 0 = expirado. */
    private Integer  remainingSeconds;
    /** Duración total del test en segundos (null en práctica). */
    private Integer  durationSeconds;
    /** true si el intento expiró por tiempo y quedó en ABANDONED. */
    private boolean  expired;
    /** true si el intento ya no está IN_PROGRESS (finalizado/abandonado/anulado). */
    private boolean  closed;

    public static TriedResumeDto of(Tried t, Integer remaining, Integer duration,
                                    boolean expired, boolean closed) {
        return TriedResumeDto.builder()
                .tried(TriedDto.from(t))
                .remainingSeconds(remaining)
                .durationSeconds(duration)
                .expired(expired)
                .closed(closed)
                .build();
    }
}
