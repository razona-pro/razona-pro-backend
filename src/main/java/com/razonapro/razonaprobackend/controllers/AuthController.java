package com.razonapro.razonaprobackend.controllers;

import com.razonapro.razonaprobackend.dtos.request.*;
import com.razonapro.razonaprobackend.dtos.response.ApiResponse;
import com.razonapro.razonaprobackend.dtos.response.AuthResponse;
import com.razonapro.razonaprobackend.dtos.response.StudentDto;
import com.razonapro.razonaprobackend.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Autenticación y gestión de credenciales")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login unificado para admins y estudiantes")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody UnifiedLoginRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(req)));
    }

    @PostMapping("/student/register")
    @Operation(summary = "Registro de estudiante")
    public ResponseEntity<ApiResponse<StudentDto>> register(
            @Valid @RequestBody StudentRegisterRequest req) {
        StudentDto dto = authService.registerStudent(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registro exitoso. Revisa tu correo para verificar tu cuenta.", dto));
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verificación de email con token enviado por correo")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.ok("Correo verificado exitosamente. Ya puedes iniciar sesión."));
    }

    @PostMapping("/student/forgot-password")
    @Operation(summary = "Solicitar recuperación de contraseña — Estudiante")
    public ResponseEntity<ApiResponse<Void>> studentForgotPassword(
            @Valid @RequestBody ForgotPasswordRequest req) {
        authService.studentForgotPassword(req);
        return ResponseEntity.ok(ApiResponse.ok(
                "Si el correo está registrado, recibirás un enlace para restablecer tu contraseña."
        ));
    }

    @PostMapping("/student/reset-password")
    @Operation(summary = "Restablecer contraseña — Estudiante")
    public ResponseEntity<ApiResponse<Void>> studentResetPassword(
            @Valid @RequestBody ResetPasswordRequest req) {
        authService.studentResetPassword(req);
        return ResponseEntity.ok(ApiResponse.ok("Contraseña actualizada exitosamente."));
    }

    @PostMapping("/admin/forgot-password")
    @Operation(summary = "Solicitar recuperación de contraseña — Admin")
    public ResponseEntity<ApiResponse<Void>> adminForgotPassword(
            @Valid @RequestBody AdminForgotPasswordRequest req) {
        authService.adminForgotPassword(req);
        return ResponseEntity.ok(ApiResponse.ok(
                "Si las credenciales son correctas, recibirás un enlace para restablecer tu contraseña."
        ));
    }

    @PostMapping("/admin/reset-password")
    @Operation(summary = "Restablecer contraseña — Admin")
    public ResponseEntity<ApiResponse<Void>> adminResetPassword(
            @Valid @RequestBody ResetPasswordRequest req) {
        authService.adminResetPassword(req);
        return ResponseEntity.ok(ApiResponse.ok("Contraseña actualizada exitosamente."));
    }
}