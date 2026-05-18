package com.razonapro.razonaprobackend.controllers;

import com.razonapro.razonaprobackend.dtos.request.ProgramRequest;
import com.razonapro.razonaprobackend.dtos.response.ApiResponse;
import com.razonapro.razonaprobackend.dtos.response.ProgramDto;
import com.razonapro.razonaprobackend.services.ProgramService;
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
public class ProgramController {

    private final ProgramService programService;

    /** Cualquiera puede ver los programas activos (para el registro) */
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
