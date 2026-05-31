package com.razonapro.razonaprobackend.domain.competence.controller;

import com.razonapro.razonaprobackend.domain.competence.dto.request.CompetenceRequest;
import com.razonapro.razonaprobackend.domain.competence.dto.response.CompetenceDto;
import com.razonapro.razonaprobackend.domain.competence.service.CompetenceService;
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
@RequestMapping("/api/competences")
@RequiredArgsConstructor
@Tag(name = "Competences", description = "Competencias evaluadas")
public class CompetenceController {

    private final CompetenceService competenceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CompetenceDto>>> findAll(@RequestParam(required = false) Boolean active) {
        List<CompetenceDto> data = Boolean.TRUE.equals(active)
                ? competenceService.findAllActive()
                : competenceService.findAll();
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompetenceDto>> findById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(competenceService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CompetenceDto>> create(@Valid @RequestBody CompetenceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(competenceService.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CompetenceDto>> update(
            @PathVariable String id, @Valid @RequestBody CompetenceRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(competenceService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable String id) {
        competenceService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok("Competencia desactivada"));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CompetenceDto>> activate(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(competenceService.activate(id)));
    }
}