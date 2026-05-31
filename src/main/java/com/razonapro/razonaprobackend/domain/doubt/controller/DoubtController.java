// domain/doubt/controller/DoubtController.java
package com.razonapro.razonaprobackend.domain.doubt.controller;

import com.razonapro.razonaprobackend.domain.doubt.dto.DoubtDto;
import com.razonapro.razonaprobackend.domain.doubt.dto.DoubtRequest;
import com.razonapro.razonaprobackend.domain.doubt.service.DoubtService;
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
@RequestMapping("/api/doubts")
@RequiredArgsConstructor
@Tag(name = "Doubts", description = "Reportes de duda sobre preguntas")
public class DoubtController {

    private final DoubtService service;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<DoubtDto>> report(
            @Valid @RequestBody DoubtRequest req, @AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Duda reportada. Un administrador la revisará.", service.report(req, p)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<DoubtDto>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.findAll(status, PageRequest.of(page, size))));
    }

    @PutMapping("/{doubtId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DoubtDto>> updateStatus(
            @PathVariable String doubtId, @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.ok(service.updateStatus(doubtId, status)));
    }
}