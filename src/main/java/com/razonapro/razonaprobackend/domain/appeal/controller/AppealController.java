package com.razonapro.razonaprobackend.domain.appeal.controller;

import com.razonapro.razonaprobackend.domain.appeal.dto.*;
import com.razonapro.razonaprobackend.domain.appeal.service.AppealService;
import com.razonapro.razonaprobackend.infrastructure.security.UserPrincipal;
import com.razonapro.razonaprobackend.shared.dto.ApiResponse;
import com.razonapro.razonaprobackend.shared.dto.PagedResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appeals")
@RequiredArgsConstructor
@Tag(name = "Appeals", description = "Apelaciones de cuentas desactivadas")
public class AppealController {

    private final AppealService service;

    /** Público: estado de la cuenta (re-valida credenciales) para el flujo de apelación en login. */
    @PostMapping("/account-status")
    public ResponseEntity<ApiResponse<AccountStatusDto>> accountStatus(
            @Valid @RequestBody AccountStatusRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.accountStatus(req)));
    }

    /** Público: enviar apelación (el estudiante desactivado no tiene sesión). */
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<AppealDto>> submit(@Valid @RequestBody AppealSubmitRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Apelación enviada. Un administrador la revisará.", service.submit(req)));
    }

    /** Admin: listar apelaciones (filtro opcional por estado). */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<AppealDto>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.findAll(status, PageRequest.of(page, size))));
    }

    /** Admin: resolver una apelación (aprobar reactiva la cuenta). */
    @PutMapping("/{appealId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AppealDto>> resolve(
            @PathVariable String appealId,
            @Valid @RequestBody AppealResolveRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.resolve(appealId, req, principal.getId())));
    }
}
