package com.razonapro.razonaprobackend.domain.tried.controller;

import com.razonapro.razonaprobackend.domain.tried.dto.request.StartTriedRequest;
import com.razonapro.razonaprobackend.domain.tried.dto.request.SubmitAnswerRequest;
import com.razonapro.razonaprobackend.domain.tried.dto.response.TriedDto;
import com.razonapro.razonaprobackend.domain.tried.dto.response.TriedEligibilityDto;
import com.razonapro.razonaprobackend.domain.tried.dto.response.TriedResumeDto;
import com.razonapro.razonaprobackend.domain.tried.dto.response.TriedReviewDto;
import com.razonapro.razonaprobackend.domain.tried.service.TriedService;
import com.razonapro.razonaprobackend.infrastructure.security.UserPrincipal;
import com.razonapro.razonaprobackend.shared.dto.ApiResponse;
import com.razonapro.razonaprobackend.shared.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/trieds")
@RequiredArgsConstructor
@Tag(name = "Trieds", description = "Intentos de tests por estudiante")
public class TriedController {

    private final TriedService triedService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Historial de intentos de todos los estudiantes (studentId y status son filtros opcionales)")
    public ResponseEntity<ApiResponse<PagedResponse<TriedDto>>> findAll(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(triedService.findAllForAdmin(studentId, status,
                PageRequest.of(page, size, Sort.by("attemptTimestamp").descending()))));
    }

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
    @Operation(summary = "Retroalimentación post-test. El estudiante solo puede verla UNA vez; el admin siempre.")
    public ResponseEntity<ApiResponse<TriedReviewDto>> getReview(
            @PathVariable String triedId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(triedService.getReview(triedId, principal)));
    }

    @PostMapping("/{triedId}/forfeit-review")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "El estudiante renuncia a su retroalimentación de un solo uso (al salir sin verla)")
    public ResponseEntity<ApiResponse<Void>> forfeitReview(
            @PathVariable String triedId,
            @AuthenticationPrincipal UserPrincipal principal) {
        triedService.forfeitReview(triedId, principal);
        return ResponseEntity.ok(ApiResponse.ok("Review marcado como no consultado"));
    }

    @GetMapping("/{triedId}/breakdown")
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    @Operation(summary = "Desglose de aciertos por competencia (sin revelar respuestas; lo ve el estudiante dueño)")
    public ResponseEntity<ApiResponse<java.util.List<com.razonapro.razonaprobackend.domain.tried.dto.response.CompetenceBreakdownDto>>> breakdown(
            @PathVariable String triedId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(triedService.getCompetenceBreakdown(triedId, principal)));
    }

    @GetMapping("/eligibility")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Indica si el estudiante puede entrar a una prueba (y si hay un intento activo para reanudar)")
    public ResponseEntity<ApiResponse<TriedEligibilityDto>> eligibility(
            @RequestParam String testId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                triedService.checkEligibility(testId, principal)));
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

    @GetMapping("/{triedId}/resume")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Reanuda un intento; el tiempo corre desde el inicio (server-authoritative)")
    public ResponseEntity<ApiResponse<TriedResumeDto>> resume(
            @PathVariable String triedId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(triedService.resume(triedId, principal)));
    }

    @PostMapping("/{triedId}/fraud")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Registra un evento sospechoso; anula el intento si supera el límite")
    public ResponseEntity<ApiResponse<TriedDto>> reportFraud(
            @PathVariable String triedId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(triedService.reportFraud(triedId, principal)));
    }

    @PutMapping("/{triedId}/finish")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<TriedDto>> finish(
            @PathVariable String triedId,
            @RequestParam(required = false) Integer timeSpentSeconds,
            @AuthenticationPrincipal UserPrincipal principal) {
        TriedDto dto = triedService.finishManually(triedId, timeSpentSeconds, principal);
        // El ranking se actualiza por trigger al pasar el intento a FINISHED.
        return ResponseEntity.ok(ApiResponse.ok("Intento finalizado", dto));
    }
}