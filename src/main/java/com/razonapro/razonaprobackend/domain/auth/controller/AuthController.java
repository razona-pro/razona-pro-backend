package com.razonapro.razonaprobackend.domain.auth.controller;

import com.razonapro.razonaprobackend.domain.auth.dto.request.UnifiedLoginRequest;
import com.razonapro.razonaprobackend.domain.auth.dto.request.StudentRegisterRequest;
import com.razonapro.razonaprobackend.dtos.request.AdminForgotPasswordRequest;
import com.razonapro.razonaprobackend.dtos.request.ForgotPasswordRequest;
import com.razonapro.razonaprobackend.dtos.request.ResetPasswordRequest;
import com.razonapro.razonaprobackend.shared.dto.ApiResponse;
import com.razonapro.razonaprobackend.domain.auth.dto.response.AuthResponse;
import com.razonapro.razonaprobackend.domain.student.dto.response.StudentDto;
import com.razonapro.razonaprobackend.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// domain/auth/controller/AuthController.java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody UnifiedLoginRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(req)));
    }

    @PostMapping("/student/register")
    public ResponseEntity<ApiResponse<StudentDto>> register(@Valid @RequestBody StudentRegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registro exitoso. Revisa tu correo.", authService.registerStudent(req)));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.ok("Correo verificado."));
    }

    @PostMapping("/student/forgot-password")
    public ResponseEntity<ApiResponse<Void>> studentForgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.studentForgotPassword(req);
        return ResponseEntity.ok(ApiResponse.ok("Si el correo está registrado, recibirás un enlace."));
    }

    @PostMapping("/student/reset-password")
    public ResponseEntity<ApiResponse<Void>> studentResetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.studentResetPassword(req);
        return ResponseEntity.ok(ApiResponse.ok("Contraseña actualizada."));
    }

    @PostMapping("/admin/forgot-password")
    public ResponseEntity<ApiResponse<Void>> adminForgotPassword(@Valid @RequestBody AdminForgotPasswordRequest req) {
        authService.adminForgotPassword(req);
        return ResponseEntity.ok(ApiResponse.ok("Si las credenciales son correctas, recibirás un enlace."));
    }

    @PostMapping("/admin/reset-password")
    public ResponseEntity<ApiResponse<Void>> adminResetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.adminResetPassword(req);
        return ResponseEntity.ok(ApiResponse.ok("Contraseña actualizada."));
    }
}