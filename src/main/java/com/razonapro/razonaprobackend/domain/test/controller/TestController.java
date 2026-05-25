package com.razonapro.razonaprobackend.domain.test.controller;

import com.razonapro.razonaprobackend.domain.test.dto.request.TestRequest;
import com.razonapro.razonaprobackend.shared.dto.ApiResponse;
import com.razonapro.razonaprobackend.shared.dto.PagedResponse;
import com.razonapro.razonaprobackend.domain.question.dto.response.QuestionDto;
import com.razonapro.razonaprobackend.domain.test.dto.response.TestDto;
import com.razonapro.razonaprobackend.infrastructure.security.UserPrincipal;
import com.razonapro.razonaprobackend.domain.test.service.TestService;
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
public class TestController {

    private final TestService testService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public ResponseEntity<ApiResponse<PagedResponse<TestDto>>> findAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(testService.findAll(pageable, !isAdmin)));
    }

    @GetMapping("/{testId}/{competenceId}")
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public ResponseEntity<ApiResponse<TestDto>> findById(
            @PathVariable String testId, @PathVariable String competenceId) {
        return ResponseEntity.ok(ApiResponse.ok(testService.findById(testId, competenceId)));
    }

    @GetMapping("/{testId}/{competenceId}/questions")
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public ResponseEntity<ApiResponse<List<QuestionDto>>> getQuestions(
            @PathVariable String testId,
            @PathVariable String competenceId,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean showCorrect = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(ApiResponse.ok(
                testService.getTestQuestions(testId, competenceId, showCorrect)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TestDto>> create(
            @Valid @RequestBody TestRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(testService.create(req, principal)));
    }

    @PostMapping("/{testId}/{competenceId}/questions/{questionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> addQuestion(
            @PathVariable String testId,
            @PathVariable String competenceId,
            @PathVariable String questionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        testService.addQuestion(testId, competenceId, questionId, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Pregunta agregada al test"));
    }

    @DeleteMapping("/{testId}/{competenceId}/questions/{questionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeQuestion(
            @PathVariable String testId,
            @PathVariable String competenceId,
            @PathVariable String questionId) {
        testService.removeQuestion(testId, competenceId, questionId);
        return ResponseEntity.ok(ApiResponse.ok("Pregunta removida del test"));
    }

    @DeleteMapping("/{testId}/{competenceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable String testId, @PathVariable String competenceId) {
        testService.deactivate(testId, competenceId);
        return ResponseEntity.ok(ApiResponse.ok("Test desactivado"));
    }
}