package com.razonapro.razonaprobackend.domain.tried.controller;

import com.razonapro.razonaprobackend.dtos.request.StartTriedRequest;
import com.razonapro.razonaprobackend.dtos.request.SubmitAnswerRequest;
import com.razonapro.razonaprobackend.shared.dto.ApiResponse;
import com.razonapro.razonaprobackend.shared.dto.PagedResponse;
import com.razonapro.razonaprobackend.domain.tried.dto.response.TriedDto;
import com.razonapro.razonaprobackend.infrastructure.security.UserPrincipal;
import com.razonapro.razonaprobackend.domain.tried.service.TriedService;
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
@PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
public class TriedController {

    private final TriedService triedService;

    /** GET /api/trieds/my — intentos propios del estudiante */
    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<PagedResponse<TriedDto>>> findMy(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("attemptTimestamp").descending());
        return ResponseEntity.ok(ApiResponse.ok(triedService.findMyTrieds(principal, pageable)));
    }

    @GetMapping("/{triedId}")
    public ResponseEntity<ApiResponse<TriedDto>> findById(
            @PathVariable String triedId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(triedService.findById(triedId, principal)));
    }

    /** POST /api/trieds/start — iniciar intento */
    @PostMapping("/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<TriedDto>> start(
            @Valid @RequestBody StartTriedRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Intento iniciado", triedService.startTried(req, principal)));
    }

    /** POST /api/trieds/{triedId}/answer — responder una pregunta */
    @PostMapping("/{triedId}/answer")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<TriedDto>> submitAnswer(
            @PathVariable String triedId,
            @Valid @RequestBody SubmitAnswerRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(triedService.submitAnswer(triedId, req, principal)));
    }

    /** PUT /api/trieds/{triedId}/finish — finalizar intento */
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
