// domain/student/controller/StudentController.java
package com.razonapro.razonaprobackend.domain.student.controller;

import com.razonapro.razonaprobackend.domain.student.dto.response.StudentDto;
import com.razonapro.razonaprobackend.domain.student.service.StudentService;
import com.razonapro.razonaprobackend.dtos.request.StudentUpdateRequest;
import com.razonapro.razonaprobackend.shared.dto.ApiResponse;
import com.razonapro.razonaprobackend.shared.dto.PagedResponse;
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
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<StudentDto>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                studentService.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<ApiResponse<StudentDto>> findById(@PathVariable String studentId) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.findById(studentId)));
    }

    @PutMapping("/{studentId}")
    public ResponseEntity<ApiResponse<StudentDto>> update(
            @PathVariable String studentId, @Valid @RequestBody StudentUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.update(studentId, req)));
    }

    @DeleteMapping("/{studentId}")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable String studentId) {
        studentService.deactivate(studentId);
        return ResponseEntity.ok(ApiResponse.ok("Estudiante desactivado"));
    }
}