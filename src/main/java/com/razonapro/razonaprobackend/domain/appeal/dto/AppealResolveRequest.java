package com.razonapro.razonaprobackend.domain.appeal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Resolución de una apelación por un admin. */
@Getter @Setter
public class AppealResolveRequest {
    /** true = aprobar (reactiva la cuenta); false = rechazar. */
    @NotNull private Boolean approve;
    @Size(max = 1000) private String response;
}
