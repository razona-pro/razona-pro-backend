package com.razonapro.razonaprobackend.domain.admin.controller;

import com.razonapro.razonaprobackend.domain.admin.dto.request.AdminCreateRequest;
import com.razonapro.razonaprobackend.domain.admin.dto.request.AdminUpdateRequest;
import com.razonapro.razonaprobackend.domain.admin.dto.response.AdminDto;
import com.razonapro.razonaprobackend.domain.admin.service.AdminService;
import com.razonapro.razonaprobackend.infrastructure.security.UserPrincipal;
import com.razonapro.razonaprobackend.shared.dto.ApiResponse;
import com.razonapro.razonaprobackend.shared.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/api/admins")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admins", description = "Gestión de administradores")
public class AdminController {

    private final AdminService adminService;

    @GetMapping
    @Operation(summary = "Listar administradores con filtros opcionales")
    public ResponseEntity<ApiResponse<PagedResponse<AdminDto>>> findAll(
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "20") int    size,
            @RequestParam(required = false)    String search,
            @RequestParam(required = false)    String status) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminService.findAll(search, status,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener admin por ID")
    public ResponseEntity<ApiResponse<AdminDto>> findById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Crear admin")
    public ResponseEntity<ApiResponse<AdminDto>> create(@Valid @RequestBody AdminCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(adminService.create(req)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar admin")
    public ResponseEntity<ApiResponse<AdminDto>> update(
            @PathVariable String id, @Valid @RequestBody AdminUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar admin")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable String id) {
        adminService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok("Admin desactivado"));
    }

    @GetMapping("/me")
    @Operation(summary = "Perfil del admin autenticado")
    public ResponseEntity<ApiResponse<AdminDto>> me(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.findById(principal.getId())));
    }
}