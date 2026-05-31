// src/main/java/com/razonapro/razonaprobackend/domain/aitried/controller/AiTriedController.java
package com.razonapro.razonaprobackend.domain.aitried.controller;

import com.razonapro.razonaprobackend.domain.aitried.dto.request.AiHintRequest;
import com.razonapro.razonaprobackend.domain.aitried.dto.request.StartAiTriedRequest;
import com.razonapro.razonaprobackend.domain.aitried.dto.request.SubmitAiAnswerRequest;
import com.razonapro.razonaprobackend.domain.aitried.dto.response.*;
import com.razonapro.razonaprobackend.domain.aitried.service.AiTriedService;
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
@RequestMapping("/api/ai-trieds")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
@Tag(name = "AI Trieds", description = "Sesiones de práctica adaptativa con IA")
public class AiTriedController {

    private final AiTriedService aiTriedService;

    @GetMapping("/status")
    @Operation(summary = "Estado del módulo de IA")
    public ResponseEntity<ApiResponse<AiStatusDto>> status() {
        return ResponseEntity.ok(ApiResponse.ok(aiTriedService.getStatus()));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Historial de sesiones IA del estudiante")
    public ResponseEntity<ApiResponse<PagedResponse<AiTriedDto>>> findMy(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(aiTriedService.findMy(principal,
                PageRequest.of(page, size, Sort.by("attemptTimestamp").descending()))));
    }

    @GetMapping("/{aiTriedId}")
    @Operation(summary = "Detalle de una sesión IA")
    public ResponseEntity<ApiResponse<AiTriedDto>> findById(
            @PathVariable String aiTriedId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(aiTriedService.findById(aiTriedId, principal)));
    }

    @PostMapping("/start")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Iniciar sesión adaptativa con IA — genera la primera pregunta")
    public ResponseEntity<ApiResponse<AiStartResponseDto>> start(
            @Valid @RequestBody StartAiTriedRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Sesión IA iniciada", aiTriedService.start(req, principal)));
    }

    @GetMapping("/{aiTriedId}/next")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Obtener la siguiente pregunta generada por IA")
    public ResponseEntity<ApiResponse<AiQuestionDto>> nextQuestion(
            @PathVariable String aiTriedId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                aiTriedService.nextQuestion(aiTriedId, principal)));
    }

    @PostMapping("/{aiTriedId}/answer")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Enviar respuesta — el servidor evalúa y adapta la dificultad")
    public ResponseEntity<ApiResponse<AiAnswerResultDto>> submitAnswer(
            @PathVariable String aiTriedId,
            @Valid @RequestBody SubmitAiAnswerRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                aiTriedService.submitAnswer(aiTriedId, req, principal)));
    }

    @PostMapping("/{aiTriedId}/hint")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Solicitar pista (nivel 1, 2 o 3)")
    public ResponseEntity<ApiResponse<AiHintDto>> getHint(
            @PathVariable String aiTriedId,
            @Valid @RequestBody AiHintRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                aiTriedService.getHint(aiTriedId, req, principal)));
    }

    @PutMapping("/{aiTriedId}/finish")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Finalizar sesión manualmente")
    public ResponseEntity<ApiResponse<AiTriedDto>> finish(
            @PathVariable String aiTriedId,
            @RequestParam(required = false) Integer timeSpentSeconds,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok("Sesión AI finalizada",
                aiTriedService.finish(aiTriedId, timeSpentSeconds, principal)));
    }
}