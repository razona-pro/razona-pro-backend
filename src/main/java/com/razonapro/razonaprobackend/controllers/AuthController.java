package com.razonapro.razonaprobackend.controllers;

import com.razonapro.razonaprobackend.dtos.request.UnifiedLoginRequest;
import com.razonapro.razonaprobackend.dtos.request.StudentRegisterRequest;
import com.razonapro.razonaprobackend.dtos.response.ApiResponse;
import com.razonapro.razonaprobackend.dtos.response.AuthResponse;
import com.razonapro.razonaprobackend.dtos.response.StudentDto;
import com.razonapro.razonaprobackend.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** POST /api/auth/login — unificado admin y estudiante */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody UnifiedLoginRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(req)));
    }

    /** POST /api/auth/student/register */
    @PostMapping("/student/register")
    public ResponseEntity<ApiResponse<StudentDto>> register(@Valid @RequestBody StudentRegisterRequest req) {
        StudentDto dto = authService.registerStudent(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registro exitoso. Revisa tu correo para verificar tu cuenta.", dto));
    }

    /** GET /api/auth/verify-email?token=xxx */
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.ok("¡Correo verificado exitosamente! Ya puedes iniciar sesión."));
    }
}