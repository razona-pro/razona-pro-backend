package com.razonapro.razonaprobackend.controllers;

import com.razonapro.razonaprobackend.dtos.request.QuestionRequest;
import com.razonapro.razonaprobackend.dtos.response.ApiResponse;
import com.razonapro.razonaprobackend.dtos.response.PagedResponse;
import com.razonapro.razonaprobackend.dtos.response.QuestionDto;
import com.razonapro.razonaprobackend.security.UserPrincipal;
import com.razonapro.razonaprobackend.services.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/competences/{competenceId}/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public ResponseEntity<ApiResponse<PagedResponse<QuestionDto>>> findAll(
            @PathVariable String competenceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
            questionService.findByCompetence(competenceId, PageRequest.of(page, size))));
    }

    @GetMapping("/{questionId}")
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public ResponseEntity<ApiResponse<QuestionDto>> findById(
            @PathVariable String competenceId, @PathVariable String questionId) {
        return ResponseEntity.ok(ApiResponse.ok(questionService.findById(competenceId, questionId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<QuestionDto>> create(
            @PathVariable String competenceId,
            @Valid @RequestBody QuestionRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(questionService.create(competenceId, req, principal)));
    }

    @DeleteMapping("/{questionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable String competenceId, @PathVariable String questionId) {
        questionService.deactivate(competenceId, questionId);
        return ResponseEntity.ok(ApiResponse.ok("Pregunta desactivada"));
    }
}
