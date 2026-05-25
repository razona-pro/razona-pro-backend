package com.razonapro.razonaprobackend.domain.auth.controller;

import com.razonapro.razonaprobackend.domain.auth.dto.request.ResetPasswordRequest;
import com.razonapro.razonaprobackend.domain.auth.dto.request.StudentRegisterRequest;
import com.razonapro.razonaprobackend.domain.auth.dto.request.UnifiedForgotPasswordRequest;
import com.razonapro.razonaprobackend.domain.auth.dto.request.UnifiedLoginRequest;
import com.razonapro.razonaprobackend.domain.auth.dto.response.AuthResponse;
import com.razonapro.razonaprobackend.domain.auth.service.AuthService;
import com.razonapro.razonaprobackend.domain.student.dto.response.StudentDto;
import com.razonapro.razonaprobackend.shared.dto.ApiResponse;
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
    @Operation(summary = "Login unificado admin/estudiante")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody UnifiedLoginRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(req)));
    }

    @PostMapping("/register")
    @Operation(summary = "Registro público de estudiante")
    public ResponseEntity<ApiResponse<StudentDto>> register(@Valid @RequestBody StudentRegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registro exitoso. Revisa tu correo para verificar tu cuenta.",
                        authService.registerStudent(req)));
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verificar email con token")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.ok("Correo verificado exitosamente."));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar restablecimiento (admin o estudiante). Requiere email + código + teléfono.")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody UnifiedForgotPasswordRequest req) {
        authService.forgotPassword(req);
        return ResponseEntity.ok(ApiResponse.ok(
                "Si los datos coinciden, recibirás un enlace en tu correo."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Aplicar nueva contraseña usando el token recibido por correo")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(ApiResponse.ok("Contraseña actualizada exitosamente."));
    }
}