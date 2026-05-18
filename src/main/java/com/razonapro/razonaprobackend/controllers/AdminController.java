package com.razonapro.razonaprobackend.controllers;

import com.razonapro.razonaprobackend.dtos.request.AdminCreateRequest;
import com.razonapro.razonaprobackend.dtos.request.AdminUpdateRequest;
import com.razonapro.razonaprobackend.dtos.response.AdminDto;
import com.razonapro.razonaprobackend.dtos.response.ApiResponse;
import com.razonapro.razonaprobackend.dtos.response.PagedResponse;
import com.razonapro.razonaprobackend.services.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admins")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AdminDto>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(adminService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminDto>> findById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminDto>> create(@Valid @RequestBody AdminCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(adminService.create(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminDto>> update(
            @PathVariable String id, @Valid @RequestBody AdminUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable String id) {
        adminService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok("Admin desactivado"));
    }
}
