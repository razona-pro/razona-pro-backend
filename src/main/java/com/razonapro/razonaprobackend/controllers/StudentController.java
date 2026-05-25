package com.razonapro.razonaprobackend.controllers;

import com.razonapro.razonaprobackend.dtos.request.StudentUpdateRequest;
import com.razonapro.razonaprobackend.dtos.response.ApiResponse;
import com.razonapro.razonaprobackend.dtos.response.PagedResponse;
import com.razonapro.razonaprobackend.dtos.response.StudentDto;
import com.razonapro.razonaprobackend.services.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Students", description = "Gestión de estudiantes — Solo Admin")
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    @Operation(summary = "Listar todos los estudiantes paginado")
    public ResponseEntity<ApiResponse<PagedResponse<StudentDto>>> findAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(studentService.findAll(pageable)));
    }

    @GetMapping("/{studentId}")
    @Operation(summary = "Obtener estudiante por código")
    public ResponseEntity<ApiResponse<StudentDto>> findById(@PathVariable String studentId) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.findById(studentId)));
    }

    @PutMapping("/{studentId}")
    @Operation(summary = "Actualizar datos de un estudiante")
    public ResponseEntity<ApiResponse<StudentDto>> update(
            @PathVariable String studentId,
            @Valid @RequestBody StudentUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.update(studentId, req)));
    }

    @DeleteMapping("/{studentId}")
    @Operation(summary = "Desactivar estudiante")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable String studentId) {
        studentService.deactivate(studentId);
        return ResponseEntity.ok(ApiResponse.ok("Estudiante desactivado"));
    }
}