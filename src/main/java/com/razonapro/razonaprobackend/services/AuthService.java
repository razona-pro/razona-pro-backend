package com.razonapro.razonaprobackend.services;

import com.razonapro.razonaprobackend.dtos.request.*;
import com.razonapro.razonaprobackend.dtos.response.AuthResponse;
import com.razonapro.razonaprobackend.dtos.response.StudentDto;
import com.razonapro.razonaprobackend.exception.ApiException;
import com.razonapro.razonaprobackend.models.*;
import com.razonapro.razonaprobackend.models.enums.AdminTokenType;
import com.razonapro.razonaprobackend.models.enums.StudentTokenType;
import com.razonapro.razonaprobackend.repositories.*;
import com.razonapro.razonaprobackend.security.JwtService;
import com.razonapro.razonaprobackend.util.TokenHashUtil;
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

    // ─────────────────────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(UnifiedLoginRequest req) {
        String code  = req.getCode().trim().toUpperCase();
        String email = req.getEmail().trim().toUpperCase();

        if (code.matches("^[A-Z]{3}[0-9]{3}$")) {
            return handleAdminLogin(email, req.getPassword(), code);
        }
        if (code.matches("^[0-9]{7}$")) {
            return handleStudentLogin(email, req.getPassword(), code);
        }
        throw new ApiException(
                "Código inválido. Administrador: ej AMN001 — Estudiante: ej 0192250",
                HttpStatus.UNAUTHORIZED
        );
    }

    private AuthResponse handleAdminLogin(String email, String password, String code) {
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

        return new AuthResponse(
                jwtService.generateAdminToken(admin.getAdminId(), admin.getEmail())
        );
    }

    private AuthResponse handleStudentLogin(String email, String password, String code) {
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

        return new AuthResponse(
                jwtService.generateStudentToken(
                        student.getStudentId(), student.getProgramId(), student.getEmail()
                )
        );
    }

    // ─────────────────────────────────────────────────────────────
    // REGISTRO ESTUDIANTE
    // ─────────────────────────────────────────────────────────────

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
                .studentId(studentId)
                .programId(programId)
                .firstName(req.getFirstName().trim().toUpperCase())
                .secondName(req.getSecondName() != null ? req.getSecondName().trim().toUpperCase() : null)
                .firstSurname(req.getFirstSurname().trim().toUpperCase())
                .secondSurname(req.getSecondSurname() != null ? req.getSecondSurname().trim().toUpperCase() : null)
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .build();

        studentRepository.save(student);

        String rawToken = sendEmailVerification(student);
        // rawToken solo sale por email, nunca en la respuesta HTTP
        return StudentDto.from(student);
    }

    // ─────────────────────────────────────────────────────────────
    // VERIFICACIÓN DE EMAIL
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void verifyEmail(String rawToken) {
        String hash = TokenHashUtil.hash(rawToken);

        StudentToken tokenEntity = studentTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException("Token inválido o expirado", HttpStatus.BAD_REQUEST));

        if (tokenEntity.getTokenType() != StudentTokenType.EMAIL_VERIFY)
            throw new ApiException("Token inválido", HttpStatus.BAD_REQUEST);

        if (!tokenEntity.isValid())
            throw new ApiException("Token expirado o ya utilizado", HttpStatus.BAD_REQUEST);

        Student student = studentRepository.findByStudentId(tokenEntity.getStudentId())
                .orElseThrow(() -> new ApiException("Estudiante no encontrado", HttpStatus.NOT_FOUND));

        if (Boolean.TRUE.equals(student.getEmailVerified()))
            throw new ApiException("El correo ya fue verificado");

        student.setEmailVerified(true);
        studentRepository.save(student);

        tokenEntity.setUsedAt(LocalDateTime.now());
        studentTokenRepository.save(tokenEntity);

        emailService.sendWelcomeEmail(student.getEmail(), student.getFirstName());
    }

    // ─────────────────────────────────────────────────────────────
    // RECUPERAR CONTRASEÑA — ESTUDIANTE
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void studentForgotPassword(ForgotPasswordRequest req) {
        String email = req.getEmail().trim().toUpperCase();
        // No revelar si el email existe o no — siempre respuesta genérica
        studentRepository.findByEmail(email).ifPresent(student -> {
            studentTokenRepository.invalidateAllByStudentAndType(
                    student.getStudentId(), StudentTokenType.PASSWORD_RESET
            );
            String rawToken = generateAndSaveStudentToken(
                    student.getStudentId(), StudentTokenType.PASSWORD_RESET, passwordResetExpirationMs
            );
            emailService.sendPasswordResetEmail(student.getEmail(), student.getFirstName(), rawToken);
        });
    }

    @Transactional
    public void studentResetPassword(ResetPasswordRequest req) {
        String hash = TokenHashUtil.hash(req.getToken());

        StudentToken tokenEntity = studentTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException("Token inválido o expirado", HttpStatus.BAD_REQUEST));

        if (tokenEntity.getTokenType() != StudentTokenType.PASSWORD_RESET)
            throw new ApiException("Token inválido", HttpStatus.BAD_REQUEST);

        if (!tokenEntity.isValid())
            throw new ApiException("Token expirado o ya utilizado", HttpStatus.BAD_REQUEST);

        Student student = studentRepository.findByStudentId(tokenEntity.getStudentId())
                .orElseThrow(() -> new ApiException("Estudiante no encontrado", HttpStatus.NOT_FOUND));

        student.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        studentRepository.save(student);

        tokenEntity.setUsedAt(LocalDateTime.now());
        studentTokenRepository.save(tokenEntity);

        // Invalidar cualquier otro token de reset pendiente
        studentTokenRepository.invalidateAllByStudentAndType(
                student.getStudentId(), StudentTokenType.PASSWORD_RESET
        );
    }

    // ─────────────────────────────────────────────────────────────
    // RECUPERAR CONTRASEÑA — ADMIN
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void adminForgotPassword(AdminForgotPasswordRequest req) {
        String email   = req.getEmail().trim().toUpperCase();
        String adminId = req.getAdminId().trim().toUpperCase();

        adminRepository.findByEmail(email).ifPresent(admin -> {
            if (!admin.getAdminId().equalsIgnoreCase(adminId)) return;

            adminTokenRepository.invalidateAllByAdminAndType(
                    admin.getAdminId(), AdminTokenType.PASSWORD_RESET
            );
            String rawToken = generateAndSaveAdminToken(
                    admin.getAdminId(), AdminTokenType.PASSWORD_RESET, passwordResetExpirationMs
            );
            emailService.sendPasswordResetEmail(admin.getEmail(), admin.getFirstName(), rawToken);
        });
    }

    @Transactional
    public void adminResetPassword(ResetPasswordRequest req) {
        String hash = TokenHashUtil.hash(req.getToken());

        AdminToken tokenEntity = adminTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException("Token inválido o expirado", HttpStatus.BAD_REQUEST));

        if (tokenEntity.getTokenType() != AdminTokenType.PASSWORD_RESET)
            throw new ApiException("Token inválido", HttpStatus.BAD_REQUEST);

        if (!tokenEntity.isValid())
            throw new ApiException("Token expirado o ya utilizado", HttpStatus.BAD_REQUEST);

        Admin admin = adminRepository.findById(tokenEntity.getAdminId())
                .orElseThrow(() -> new ApiException("Admin no encontrado", HttpStatus.NOT_FOUND));

        admin.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        adminRepository.save(admin);

        tokenEntity.setUsedAt(LocalDateTime.now());
        adminTokenRepository.save(tokenEntity);

        adminTokenRepository.invalidateAllByAdminAndType(
                admin.getAdminId(), AdminTokenType.PASSWORD_RESET
        );
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS INTERNOS
    // ─────────────────────────────────────────────────────────────

    private String sendEmailVerification(Student student) {
        studentTokenRepository.invalidateAllByStudentAndType(
                student.getStudentId(), StudentTokenType.EMAIL_VERIFY
        );
        String rawToken = generateAndSaveStudentToken(
                student.getStudentId(), StudentTokenType.EMAIL_VERIFY, emailVerifyExpirationMs
        );
        emailService.sendVerificationEmail(student.getEmail(), student.getFirstName(), rawToken);
        return rawToken;
    }

    private String generateAndSaveStudentToken(
            String studentId, StudentTokenType type, long expirationMs) {
        String rawToken = TokenHashUtil.generate();
        StudentToken token = StudentToken.builder()
                .studentId(studentId)
                .tokenHash(TokenHashUtil.hash(rawToken))
                .tokenType(type)
                .expiresAt(LocalDateTime.now().plusSeconds(expirationMs / 1000))
                .build();
        studentTokenRepository.save(token);
        return rawToken;
    }

    private String generateAndSaveAdminToken(
            String adminId, AdminTokenType type, long expirationMs) {
        String rawToken = TokenHashUtil.generate();
        AdminToken token = AdminToken.builder()
                .adminId(adminId)
                .tokenHash(TokenHashUtil.hash(rawToken))
                .tokenType(type)
                .expiresAt(LocalDateTime.now().plusSeconds(expirationMs / 1000))
                .build();
        adminTokenRepository.save(token);
        return rawToken;
    }
}