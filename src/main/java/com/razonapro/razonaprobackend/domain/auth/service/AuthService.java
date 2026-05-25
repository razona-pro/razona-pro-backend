// domain/auth/service/AuthService.java
package com.razonapro.razonaprobackend.domain.auth.service;

import com.razonapro.razonaprobackend.domain.admin.model.Admin;
import com.razonapro.razonaprobackend.domain.admin.repository.AdminRepository;
import com.razonapro.razonaprobackend.domain.auth.dto.request.StudentRegisterRequest;
import com.razonapro.razonaprobackend.domain.auth.dto.request.UnifiedLoginRequest;
import com.razonapro.razonaprobackend.domain.program.repository.ProgramRepository;
import com.razonapro.razonaprobackend.domain.student.dto.response.StudentDto;
import com.razonapro.razonaprobackend.domain.student.model.Student;
import com.razonapro.razonaprobackend.domain.student.repository.StudentRepository;
import com.razonapro.razonaprobackend.domain.auth.dto.request.AdminForgotPasswordRequest;
import com.razonapro.razonaprobackend.domain.auth.dto.request.ForgotPasswordRequest;
import com.razonapro.razonaprobackend.domain.auth.dto.request.ResetPasswordRequest;
import com.razonapro.razonaprobackend.infrastructure.security.JwtService;
import com.razonapro.razonaprobackend.domain.auth.model.AdminToken;
import com.razonapro.razonaprobackend.domain.auth.model.StudentToken;
import com.razonapro.razonaprobackend.domain.auth.model.enums.AdminTokenType;
import com.razonapro.razonaprobackend.domain.auth.model.enums.StudentTokenType;
import com.razonapro.razonaprobackend.domain.auth.repository.AdminTokenRepository;
import com.razonapro.razonaprobackend.domain.auth.repository.StudentTokenRepository;
import com.razonapro.razonaprobackend.infrastructure.email.EmailService;
import com.razonapro.razonaprobackend.shared.exception.ApiException;
import com.razonapro.razonaprobackend.infrastructure.util.TokenHashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdminRepository        adminRepository;
    private final StudentRepository      studentRepository;
    private final ProgramRepository      programRepository;
    private final AdminTokenRepository   adminTokenRepository;
    private final StudentTokenRepository studentTokenRepository;
    private final JwtService             jwtService;
    private final PasswordEncoder        passwordEncoder;
    private final EmailService           emailService;

    @Value("${jwt.email-verification-expiration-ms}")
    private long emailVerifyExpirationMs;

    @Value("${jwt.password-reset-expiration-ms}")
    private long passwordResetExpirationMs;

    public String login(UnifiedLoginRequest req) {
        // misma lógica pero retornar solo el token string
        String code  = req.getCode().trim().toUpperCase();
        String email = req.getEmail().trim().toUpperCase();
        if (code.matches("^[A-Z]{3}[0-9]{3}$")) return handleAdminLogin(email, req.getPassword(), code);
        if (code.matches("^[0-9]{7}$"))           return handleStudentLogin(email, req.getPassword(), code);
        throw new ApiException("Código inválido", HttpStatus.UNAUTHORIZED);
    }

    private String handleAdminLogin(String email, String password, String code) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Credenciales incorrectas", HttpStatus.UNAUTHORIZED));
        if (!admin.getAdminId().equalsIgnoreCase(code))
            throw new ApiException("Credenciales incorrectas", HttpStatus.UNAUTHORIZED);
        if (!Boolean.TRUE.equals(admin.getIsActive()))
            throw new ApiException("Cuenta deshabilitada", HttpStatus.FORBIDDEN);
        if (!passwordEncoder.matches(password, admin.getPasswordHash()))
            throw new ApiException("Credenciales incorrectas", HttpStatus.UNAUTHORIZED);
        admin.setLastLoginAt(LocalDateTime.now());
        adminRepository.save(admin);
        return jwtService.generateAdminToken(admin.getAdminId(), admin.getEmail());
    }

    private String handleStudentLogin(String email, String password, String code) {
        Student student = studentRepository.findByStudentId(code)
                .orElseThrow(() -> new ApiException("Credenciales incorrectas", HttpStatus.UNAUTHORIZED));
        if (!student.getEmail().equalsIgnoreCase(email))
            throw new ApiException("Credenciales incorrectas", HttpStatus.UNAUTHORIZED);
        if (!Boolean.TRUE.equals(student.getIsActive()))
            throw new ApiException("Cuenta deshabilitada", HttpStatus.FORBIDDEN);
        if (!Boolean.TRUE.equals(student.getEmailVerified()))
            throw new ApiException("Debes verificar tu correo antes de iniciar sesión", HttpStatus.FORBIDDEN);
        if (!passwordEncoder.matches(password, student.getPasswordHash()))
            throw new ApiException("Credenciales incorrectas", HttpStatus.UNAUTHORIZED);
        student.setLastLoginAt(LocalDateTime.now());
        studentRepository.save(student);
        return jwtService.generateStudentToken(student.getStudentId(), student.getProgramId(), student.getEmail());
    }

    // ── Registro ──────────────────────────────────────────────
    @Transactional
    public StudentDto registerStudent(StudentRegisterRequest req) {
        String studentId = req.getStudentId().trim().toUpperCase();
        String programId = studentId.substring(0, 3);
        String email     = req.getEmail().trim().toUpperCase();
        String phone     = req.getPhone().trim();

        if (!programRepository.existsById(programId))
            throw new ApiException("El código no corresponde a ningún programa registrado");
        if (studentRepository.existsByStudentId(studentId))
            throw new ApiException("El código ya está registrado");
        if (studentRepository.existsByEmail(email))
            throw new ApiException("El email ya está registrado");
        if (studentRepository.existsByPhone(phone))
            throw new ApiException("El teléfono ya está registrado");

        Student student = Student.builder()
                .studentId(studentId).programId(programId)
                .firstName(req.getFirstName().trim().toUpperCase())
                .secondName(req.getSecondName() != null ? req.getSecondName().trim().toUpperCase() : null)
                .firstSurname(req.getFirstSurname().trim().toUpperCase())
                .secondSurname(req.getSecondSurname() != null ? req.getSecondSurname().trim().toUpperCase() : null)
                .email(email).phone(phone)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .build();
        studentRepository.save(student);

        String rawToken = generateAndSaveStudentToken(studentId, StudentTokenType.EMAIL_VERIFY, emailVerifyExpirationMs);
        emailService.sendVerificationEmail(email, student.getFirstName(), rawToken);
        return StudentDto.from(student);
    }

    // ── Verificar email ───────────────────────────────────────
    @Transactional
    public void verifyEmail(String rawToken) {
        String hash = TokenHashUtil.hash(rawToken);
        StudentToken token = studentTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException("Token inválido o expirado", HttpStatus.BAD_REQUEST));
        if (token.getTokenType() != StudentTokenType.EMAIL_VERIFY)
            throw new ApiException("Token inválido", HttpStatus.BAD_REQUEST);
        if (!token.isValid())
            throw new ApiException("Token expirado o ya utilizado", HttpStatus.BAD_REQUEST);

        Student student = studentRepository.findByStudentId(token.getStudentId())
                .orElseThrow(() -> new ApiException("Estudiante no encontrado", HttpStatus.NOT_FOUND));
        if (Boolean.TRUE.equals(student.getEmailVerified()))
            throw new ApiException("El correo ya fue verificado");

        student.setEmailVerified(true);
        studentRepository.save(student);
        token.setUsedAt(LocalDateTime.now());
        studentTokenRepository.save(token);
        emailService.sendWelcomeEmail(student.getEmail(), student.getFirstName());
    }

    // ── Forgot/Reset contraseña — Estudiante ──────────────────
    @Transactional
    public void studentForgotPassword(ForgotPasswordRequest req) {
        studentRepository.findByEmail(req.getEmail().trim().toUpperCase()).ifPresent(student -> {
            studentTokenRepository.invalidateAllByStudentAndType(student.getStudentId(), StudentTokenType.PASSWORD_RESET);
            String raw = generateAndSaveStudentToken(student.getStudentId(), StudentTokenType.PASSWORD_RESET, passwordResetExpirationMs);
            emailService.sendPasswordResetEmail(student.getEmail(), student.getFirstName(), raw);
        });
    }

    @Transactional
    public void studentResetPassword(ResetPasswordRequest req) {
        StudentToken token = studentTokenRepository.findByTokenHash(TokenHashUtil.hash(req.getToken()))
                .orElseThrow(() -> new ApiException("Token inválido o expirado", HttpStatus.BAD_REQUEST));
        if (token.getTokenType() != StudentTokenType.PASSWORD_RESET || !token.isValid())
            throw new ApiException("Token inválido o expirado", HttpStatus.BAD_REQUEST);

        Student student = studentRepository.findByStudentId(token.getStudentId())
                .orElseThrow(() -> new ApiException("Estudiante no encontrado", HttpStatus.NOT_FOUND));
        student.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        studentRepository.save(student);
        token.setUsedAt(LocalDateTime.now());
        studentTokenRepository.save(token);
        studentTokenRepository.invalidateAllByStudentAndType(student.getStudentId(), StudentTokenType.PASSWORD_RESET);
    }

    // ── Forgot/Reset contraseña — Admin ───────────────────────
    @Transactional
    public void adminForgotPassword(AdminForgotPasswordRequest req) {
        String email = req.getEmail().trim().toUpperCase();
        adminRepository.findByEmail(email).ifPresent(admin -> {
            if (!admin.getAdminId().equalsIgnoreCase(req.getAdminId().trim())) return;
            adminTokenRepository.invalidateAllByAdminAndType(admin.getAdminId(), AdminTokenType.PASSWORD_RESET);
            String raw = generateAndSaveAdminToken(admin.getAdminId(), AdminTokenType.PASSWORD_RESET, passwordResetExpirationMs);
            emailService.sendPasswordResetEmail(admin.getEmail(), admin.getFirstName(), raw);
        });
    }

    @Transactional
    public void adminResetPassword(ResetPasswordRequest req) {
        AdminToken token = adminTokenRepository.findByTokenHash(TokenHashUtil.hash(req.getToken()))
                .orElseThrow(() -> new ApiException("Token inválido o expirado", HttpStatus.BAD_REQUEST));
        if (token.getTokenType() != AdminTokenType.PASSWORD_RESET || !token.isValid())
            throw new ApiException("Token inválido o expirado", HttpStatus.BAD_REQUEST);

        Admin admin = adminRepository.findById(token.getAdminId())
                .orElseThrow(() -> new ApiException("Admin no encontrado", HttpStatus.NOT_FOUND));
        admin.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        adminRepository.save(admin);
        token.setUsedAt(LocalDateTime.now());
        adminTokenRepository.save(token);
        adminTokenRepository.invalidateAllByAdminAndType(admin.getAdminId(), AdminTokenType.PASSWORD_RESET);
    }

    // ── Helpers ───────────────────────────────────────────────
    private String generateAndSaveStudentToken(String studentId, StudentTokenType type, long expirationMs) {
        String raw = TokenHashUtil.generate();
        studentTokenRepository.save(StudentToken.builder()
                .studentId(studentId).tokenHash(TokenHashUtil.hash(raw)).tokenType(type)
                .expiresAt(LocalDateTime.now().plusSeconds(expirationMs / 1000)).build());
        return raw;
    }

    private String generateAndSaveAdminToken(String adminId, AdminTokenType type, long expirationMs) {
        String raw = TokenHashUtil.generate();
        adminTokenRepository.save(AdminToken.builder()
                .adminId(adminId).tokenHash(TokenHashUtil.hash(raw)).tokenType(type)
                .expiresAt(LocalDateTime.now().plusSeconds(expirationMs / 1000)).build());
        return raw;
    }
}