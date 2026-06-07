package com.razonapro.razonaprobackend.domain.aitried.controller;

import com.razonapro.razonaprobackend.domain.aitried.dto.request.AiHintRequest;
import com.razonapro.razonaprobackend.domain.aitried.dto.request.StartAiTriedRequest;
import com.razonapro.razonaprobackend.domain.aitried.dto.request.SubmitAiAnswerRequest;
import com.razonapro.razonaprobackend.domain.aitried.dto.response.*;
import com.razonapro.razonaprobackend.domain.aitried.service.AiTriedService;
import com.razonapro.razonaprobackend.domain.ranking.service.RankingService;
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

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/ai-trieds")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
@Tag(name = "AI Trieds", description = "Práctica IA adaptativa")
public class AiTriedController {

    private final AiTriedService service;
    private final RankingService rankingService;

    /** Refresca el ranking sin afectar la respuesta: si falla, solo se loguea. */
    private void refreshRanking(UserPrincipal p) {
        try { rankingService.refreshForStudent(p.getId(), p.getProgramId(), null); }
        catch (Exception e) { log.warn("No se pudo refrescar el ranking de {}: {}", p.getId(), e.getMessage()); }
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<AiStatusDto>> status() {
        return ResponseEntity.ok(ApiResponse.ok(service.getStatus()));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<PagedResponse<AiTriedDto>>> findMy(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.findMy(p,
                PageRequest.of(page, size, Sort.by("attemptTimestamp").descending()))));
    }

    @GetMapping("/{aiTriedId}")
    public ResponseEntity<ApiResponse<AiTriedDto>> findById(
            @PathVariable String aiTriedId, @AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(ApiResponse.ok(service.findById(aiTriedId, p)));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: historial de todas las sesiones IA (studentId es filtro opcional)")
    public ResponseEntity<ApiResponse<PagedResponse<AiTriedDto>>> findAll(
            @RequestParam(required = false) String studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.findAllForAdmin(studentId,
                PageRequest.of(page, size, Sort.by("attemptTimestamp").descending()))));
    }

    @GetMapping("/student/{programId}/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: intentos IA de un estudiante")
    public ResponseEntity<ApiResponse<PagedResponse<AiTriedDto>>> findByStudent(
            @PathVariable String programId, @PathVariable String studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.findByStudent(programId, studentId,
                PageRequest.of(page, size, Sort.by("attemptTimestamp").descending()))));
    }

    @GetMapping("/{aiTriedId}/admin-questions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: preguntas que la IA generó en un intento (correcta revelada)")
    public ResponseEntity<ApiResponse<List<AiQuestionDto>>> adminQuestions(@PathVariable String aiTriedId) {
        return ResponseEntity.ok(ApiResponse.ok(service.questionsForAdmin(aiTriedId)));
    }

    @GetMapping("/{aiTriedId}/questions")
    @Operation(summary = "Lista preguntas generadas hasta ahora (sin revelar correcta si no respondida)")
    public ResponseEntity<ApiResponse<List<AiQuestionDto>>> listQuestions(
            @PathVariable String aiTriedId, @AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(ApiResponse.ok(service.listQuestions(aiTriedId, p)));
    }

    @GetMapping("/{aiTriedId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Historial detallado con respuestas y explicaciones (solo administradores)")
    public ResponseEntity<ApiResponse<List<AiQuestionDto>>> review(
            @PathVariable String aiTriedId, @AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(ApiResponse.ok(service.getReview(aiTriedId, p)));
    }

    @PostMapping("/start")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Inicia sesión adaptativa - genera SÓLO la primera pregunta")
    public ResponseEntity<ApiResponse<AiStartResponseDto>> start(
            @Valid @RequestBody StartAiTriedRequest req, @AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Sesión IA iniciada", service.start(req, p)));
    }

    @PostMapping("/{aiTriedId}/next-question")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Genera la siguiente pregunta adaptativa según el rendimiento actual")
    public ResponseEntity<ApiResponse<AiQuestionDto>> nextQuestion(
            @PathVariable String aiTriedId, @AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(ApiResponse.ok(service.generateNextQuestion(aiTriedId, p)));
    }

    @PostMapping("/{aiTriedId}/answer")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<AiAnswerResultDto>> answer(
            @PathVariable String aiTriedId, @Valid @RequestBody SubmitAiAnswerRequest req,
            @AuthenticationPrincipal UserPrincipal p) {
        AiAnswerResultDto result = service.submitAnswer(aiTriedId, req, p);
        if (result.finished()) refreshRanking(p);   // ya comprometido; refresco best-effort
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/{aiTriedId}/hint")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<AiHintDto>> hint(
            @PathVariable String aiTriedId, @Valid @RequestBody AiHintRequest req,
            @AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(ApiResponse.ok(service.getHint(aiTriedId, req, p)));
    }

    @PutMapping("/{aiTriedId}/finish")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<AiTriedDto>> finish(
            @PathVariable String aiTriedId,
            @RequestParam(required = false) Integer timeSpentSeconds,
            @AuthenticationPrincipal UserPrincipal p) {
        AiTriedDto dto = service.finish(aiTriedId, timeSpentSeconds, p);
        refreshRanking(p);
        return ResponseEntity.ok(ApiResponse.ok("Sesión finalizada", dto));
    }
}