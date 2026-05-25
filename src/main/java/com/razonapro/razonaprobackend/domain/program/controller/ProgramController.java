package com.razonapro.razonaprobackend.domain.program.controller;

import com.razonapro.razonaprobackend.domain.program.dto.request.ProgramRequest;
import com.razonapro.razonaprobackend.domain.program.dto.response.ProgramDto;
import com.razonapro.razonaprobackend.domain.program.service.ProgramService;
import com.razonapro.razonaprobackend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/programs")
@RequiredArgsConstructor
@Tag(name = "Programs", description = "Gestión de programas académicos")
public class ProgramController {

    private final ProgramService programService;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ProgramDto>>> findAllActive() {
        return ResponseEntity.ok(ApiResponse.ok(programService.findAllActive()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ProgramDto>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(programService.findAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProgramDto>> findById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(programService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProgramDto>> create(@Valid @RequestBody ProgramRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(programService.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProgramDto>> update(
            @PathVariable String id, @Valid @RequestBody ProgramRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(programService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable String id) {
        programService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok("Programa desactivado"));
    }
}