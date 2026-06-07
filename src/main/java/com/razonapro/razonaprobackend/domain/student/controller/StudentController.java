package com.razonapro.razonaprobackend.domain.student.controller;

import com.razonapro.razonaprobackend.domain.student.dto.request.StudentUpdateRequest;
import com.razonapro.razonaprobackend.domain.student.dto.response.StudentDto;
import com.razonapro.razonaprobackend.domain.student.service.StudentService;
import com.razonapro.razonaprobackend.infrastructure.security.UserPrincipal;
import com.razonapro.razonaprobackend.shared.dto.ApiResponse;
import com.razonapro.razonaprobackend.shared.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Tag(name = "Students", description = "Gestión de estudiantes")
public class StudentController {

    private final StudentService studentService;

    @PutMapping("/{studentId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StudentDto>> activate(@PathVariable String studentId) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.activate(studentId)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Perfil del estudiante autenticado")
    public ResponseEntity<ApiResponse<StudentDto>> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.findById(principal.getId())));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "El estudiante edita su propio perfil (nombres y celular)")
    public ResponseEntity<ApiResponse<StudentDto>> updateMe(
            @Valid @RequestBody StudentUpdateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                studentService.updateOwnProfile(principal.getId(), req)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar estudiantes con filtros opcionales (Admin)")
    public ResponseEntity<ApiResponse<PagedResponse<StudentDto>>> findAll(
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "20") int    size,
            @RequestParam(required = false)    String search,
            @RequestParam(required = false)    String programId,
            @RequestParam(required = false)    String status) {
        return ResponseEntity.ok(ApiResponse.ok(
                studentService.findAll(search, programId, status,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @GetMapping("/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener estudiante por código (Admin)")
    public ResponseEntity<ApiResponse<StudentDto>> findById(@PathVariable String studentId) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.findById(studentId)));
    }

    @PutMapping("/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar estudiante (Admin)")
    public ResponseEntity<ApiResponse<StudentDto>> update(
            @PathVariable String studentId, @Valid @RequestBody StudentUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.update(studentId, req)));
    }

    @DeleteMapping("/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar estudiante (Admin)")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable String studentId) {
        studentService.deactivate(studentId);
        return ResponseEntity.ok(ApiResponse.ok("Estudiante desactivado"));
    }
}