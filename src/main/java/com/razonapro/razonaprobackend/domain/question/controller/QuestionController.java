package com.razonapro.razonaprobackend.domain.question.controller;

import com.razonapro.razonaprobackend.domain.question.dto.request.QuestionRequest;
import com.razonapro.razonaprobackend.domain.question.dto.request.QuestionUpdateRequest;
import com.razonapro.razonaprobackend.domain.question.dto.response.QuestionDto;
import com.razonapro.razonaprobackend.domain.question.service.QuestionService;
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
@RequiredArgsConstructor
@Tag(name = "Questions", description = "Banco de preguntas")
public class QuestionController {

    private final QuestionService questionService;

    /** Global — todas las preguntas con filtros opcionales (admin) */
    @GetMapping("/api/questions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<QuestionDto>>> findAll(
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "20") int    size,
            @RequestParam(required = false)    String competenceId,
            @RequestParam(required = false)    String difficulty,
            @RequestParam(required = false)    String status,
            @RequestParam(required = false)    String search) {
        return ResponseEntity.ok(ApiResponse.ok(
                questionService.findByFilters(competenceId, difficulty, status, search,
                        PageRequest.of(page, size))));
    }

    @GetMapping("/api/competences/{competenceId}/questions")
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public ResponseEntity<ApiResponse<PagedResponse<QuestionDto>>> findByCompetence(
            @PathVariable String competenceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                questionService.findByCompetence(competenceId, PageRequest.of(page, size))));
    }

    @GetMapping("/api/competences/{competenceId}/questions/{questionId}")
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public ResponseEntity<ApiResponse<QuestionDto>> findById(
            @PathVariable String competenceId, @PathVariable String questionId) {
        return ResponseEntity.ok(ApiResponse.ok(questionService.findById(competenceId, questionId)));
    }

    @PostMapping("/api/competences/{competenceId}/questions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<QuestionDto>> create(
            @PathVariable String competenceId,
            @Valid @RequestBody QuestionRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(questionService.create(competenceId, req, principal)));
    }

    @PutMapping("/api/competences/{competenceId}/questions/{questionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<QuestionDto>> update(
            @PathVariable String competenceId, @PathVariable String questionId,
            @Valid @RequestBody QuestionUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(questionService.update(competenceId, questionId, req)));
    }

    @PutMapping("/api/competences/{competenceId}/questions/{questionId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activate(
            @PathVariable String competenceId, @PathVariable String questionId) {
        questionService.activate(competenceId, questionId);
        return ResponseEntity.ok(ApiResponse.ok("Pregunta activada"));
    }

    @DeleteMapping("/api/competences/{competenceId}/questions/{questionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable String competenceId, @PathVariable String questionId) {
        questionService.deactivate(competenceId, questionId);
        return ResponseEntity.ok(ApiResponse.ok("Pregunta desactivada"));
    }
}
