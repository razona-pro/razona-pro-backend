package com.razonapro.razonaprobackend.domain.aitried.controller;

import com.razonapro.razonaprobackend.domain.aitried.dto.request.StartAiTriedRequest;
import com.razonapro.razonaprobackend.domain.aitried.dto.request.SubmitAiAnswerRequest;
import com.razonapro.razonaprobackend.domain.aitried.dto.response.AiTriedDto;
import com.razonapro.razonaprobackend.shared.dto.ApiResponse;
import com.razonapro.razonaprobackend.shared.dto.PagedResponse;
import com.razonapro.razonaprobackend.infrastructure.security.UserPrincipal;
import com.razonapro.razonaprobackend.domain.aitried.service.AiTriedService;
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
@PreAuthorize("hasRole('STUDENT')")
public class AiTriedController {

    private final AiTriedService aiTriedService;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PagedResponse<AiTriedDto>>> findMy(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("attemptTimestamp").descending());
        return ResponseEntity.ok(ApiResponse.ok(aiTriedService.findMy(principal, pageable)));
    }

    @GetMapping("/{aiTriedId}")
    public ResponseEntity<ApiResponse<AiTriedDto>> findById(
            @PathVariable String aiTriedId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(aiTriedService.findById(aiTriedId, principal)));
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<AiTriedDto>> start(
            @Valid @RequestBody StartAiTriedRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Sesión AI iniciada", aiTriedService.start(req, principal)));
    }

    @PostMapping("/{aiTriedId}/answer")
    public ResponseEntity<ApiResponse<AiTriedDto>> submitAnswer(
            @PathVariable String aiTriedId,
            @Valid @RequestBody SubmitAiAnswerRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(aiTriedService.submitAnswer(aiTriedId, req, principal)));
    }

    @PutMapping("/{aiTriedId}/finish")
    public ResponseEntity<ApiResponse<AiTriedDto>> finish(
            @PathVariable String aiTriedId,
            @RequestParam(required = false) Integer timeSpentSeconds,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
            "Sesión AI finalizada", aiTriedService.finish(aiTriedId, timeSpentSeconds, principal)));
    }
}
