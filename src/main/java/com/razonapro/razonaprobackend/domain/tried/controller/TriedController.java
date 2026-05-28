package com.razonapro.razonaprobackend.domain.tried.controller;

import com.razonapro.razonaprobackend.domain.tried.dto.request.StartTriedRequest;
import com.razonapro.razonaprobackend.domain.tried.dto.request.SubmitAnswerRequest;
import com.razonapro.razonaprobackend.domain.tried.dto.response.TriedDto;
import com.razonapro.razonaprobackend.domain.tried.dto.response.TriedReviewDto;
import com.razonapro.razonaprobackend.domain.tried.service.TriedService;
import com.razonapro.razonaprobackend.infrastructure.security.UserPrincipal;
import com.razonapro.razonaprobackend.shared.dto.ApiResponse;
import com.razonapro.razonaprobackend.shared.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trieds")
@RequiredArgsConstructor
@Tag(name = "Trieds", description = "Intentos de tests por estudiante")
public class TriedController {

    private final TriedService triedService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<PagedResponse<TriedDto>>> findMy(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                triedService.findMyTrieds(principal,
                        PageRequest.of(page, size, Sort.by("attemptTimestamp").descending()))));
    }

    @GetMapping("/{triedId}")
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public ResponseEntity<ApiResponse<TriedDto>> findById(
            @PathVariable String triedId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(triedService.findById(triedId, principal)));
    }

    /**
     * Review completo de un intento finalizado.
     * Retorna cada pregunta con opciones, selección del estudiante,
     * opción correcta y explicación pedagógica.
     */
    @GetMapping("/{triedId}/review")
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    @Operation(summary = "Post-test review con feedback completo")
    public ResponseEntity<ApiResponse<TriedReviewDto>> getReview(
            @PathVariable String triedId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(triedService.getReview(triedId, principal)));
    }

    @PostMapping("/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<TriedDto>> start(
            @Valid @RequestBody StartTriedRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Intento iniciado", triedService.startTried(req, principal)));
    }

    @PostMapping("/{triedId}/answer")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<TriedDto>> submitAnswer(
            @PathVariable String triedId,
            @Valid @RequestBody SubmitAnswerRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(triedService.submitAnswer(triedId, req, principal)));
    }

    @PutMapping("/{triedId}/finish")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<TriedDto>> finish(
            @PathVariable String triedId,
            @RequestParam(required = false) Integer timeSpentSeconds,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Intento finalizado", triedService.finishManually(triedId, timeSpentSeconds, principal)));
    }
}