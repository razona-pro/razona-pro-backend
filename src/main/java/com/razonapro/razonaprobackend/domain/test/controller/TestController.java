package com.razonapro.razonaprobackend.domain.test.controller;

import com.razonapro.razonaprobackend.domain.question.dto.response.QuestionDto;
import com.razonapro.razonaprobackend.domain.test.dto.request.TestRequest;
import com.razonapro.razonaprobackend.domain.test.dto.request.TestUpdateRequest;
import com.razonapro.razonaprobackend.domain.test.dto.response.TestDto;
import com.razonapro.razonaprobackend.domain.test.service.TestService;
import com.razonapro.razonaprobackend.infrastructure.security.UserPrincipal;
import com.razonapro.razonaprobackend.shared.dto.ApiResponse;
import com.razonapro.razonaprobackend.shared.dto.PagedResponse;
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

import java.util.List;

@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
@Tag(name = "Tests", description = "Tests y asignación de preguntas")
public class TestController {

    private final TestService testService;

    @PutMapping("/{testId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TestDto>> activate(@PathVariable String testId) {
        return ResponseEntity.ok(ApiResponse.ok(testService.activate(testId)));
    }

    /** Edita una prueba (nombre, descripción, duración, modo, nº de preguntas, estado). */
    @PutMapping("/{testId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TestDto>> update(
            @PathVariable String testId,
            @Valid @RequestBody TestUpdateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(testService.update(testId, req, principal)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public ResponseEntity<ApiResponse<PagedResponse<TestDto>>> findAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(ApiResponse.ok(
                testService.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()), !isAdmin)));
    }

    @GetMapping("/{testId}")
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public ResponseEntity<ApiResponse<TestDto>> findById(@PathVariable String testId) {
        return ResponseEntity.ok(ApiResponse.ok(testService.findById(testId)));
    }

    @GetMapping("/{testId}/questions")
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public ResponseEntity<ApiResponse<List<QuestionDto>>> getQuestions(
            @PathVariable String testId,
            @RequestParam(required = false) String triedId,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean showCorrect = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        // triedId hace la selección de preguntas DETERMINISTA por intento (consistente con
        // startTried y con las "sin responder" del cierre); sin él se devuelven todas.
        return ResponseEntity.ok(ApiResponse.ok(
                testService.getTestQuestions(testId, showCorrect, triedId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TestDto>> create(
            @Valid @RequestBody TestRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(testService.create(req, principal)));
    }

    // La competencia del path = competencia de la PREGUNTA (una prueba es multicompetencia).
    @PostMapping("/{testId}/questions/{competenceId}/{questionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> addQuestion(
            @PathVariable String testId, @PathVariable String competenceId, @PathVariable String questionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        testService.addQuestion(testId, competenceId, questionId, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Pregunta agregada al test"));
    }

    @DeleteMapping("/{testId}/questions/{competenceId}/{questionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeQuestion(
            @PathVariable String testId, @PathVariable String competenceId, @PathVariable String questionId) {
        testService.removeQuestion(testId, competenceId, questionId);
        return ResponseEntity.ok(ApiResponse.ok("Pregunta removida del test"));
    }

    @DeleteMapping("/{testId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable String testId) {
        testService.deactivate(testId);
        return ResponseEntity.ok(ApiResponse.ok("Test desactivado"));
    }
}