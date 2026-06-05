package com.razonapro.razonaprobackend.domain.auth.service;

import com.razonapro.razonaprobackend.domain.admin.model.Admin;
import com.razonapro.razonaprobackend.domain.admin.repository.AdminRepository;
import com.razonapro.razonaprobackend.domain.auth.dto.request.*;
import com.razonapro.razonaprobackend.domain.auth.dto.response.AuthResponse;
import com.razonapro.razonaprobackend.domain.auth.dto.response.ResendVerificationResponse;
import com.razonapro.razonaprobackend.domain.auth.model.AdminToken;
import com.razonapro.razonaprobackend.domain.auth.model.StudentToken;
import com.razonapro.razonaprobackend.domain.auth.model.enums.AdminTokenType;
import com.razonapro.razonaprobackend.domain.auth.model.enums.StudentTokenType;
import com.razonapro.razonaprobackend.domain.auth.repository.AdminTokenRepository;
import com.razonapro.razonaprobackend.domain.auth.repository.StudentTokenRepository;
import com.razonapro.razonaprobackend.domain.program.repository.ProgramRepository;
import com.razonapro.razonaprobackend.domain.student.dto.response.StudentDto;
import com.razonapro.razonaprobackend.domain.student.model.Student;
import com.razonapro.razonaprobackend.domain.student.repository.StudentRepository;
import com.razonapro.razonaprobackend.infrastructure.email.EmailService;
import com.razonapro.razonaprobackend.infrastructure.security.JwtService;
import com.razonapro.razonaprobackend.infrastructure.util.TokenHashUtil;
import com.razonapro.razonaprobackend.shared.exception.ApiException;
import com.razonapro.razonaprobackend.shared.exception.ErrorCode;
import com.razonapro.razonaprobackend.shared.util.StringNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    // Patrones de validación de código
    private static final String ADMIN_CODE_PATTERN   = "^[A-Za-z]{3}[0-9]{3}$";
    private static final String STUDENT_CODE_PATTERN = "^[0-9]{7}$";

    private final AdminRepository        adminRepository;
    private final StudentRepository      studentRepository;
    private final ProgramRepository      programRepository;
    private final AdminTokenRepository   adminTokenRepository;
    private final StudentTokenRepository studentTokenRepository;
    private final JwtService             jwtService;
    private final PasswordEncoder        passwordEncoder;
    private final EmailService           emailService;

    // Reenvío de verificación: 60s entre reenvíos, máx. 5 por hora
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int RESEND_MAX_PER_HOUR     = 5;

    @Value("${jwt.email-verification-expiration-ms}")
    private long emailVerifyExpirationMs;

    @Value("${jwt.password-reset-expiration-ms}")
    private long passwordResetExpirationMs;

    // ═══════════════════════════════════════════════════════════════════════
    //  LOGIN UNIFICADO
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public AuthResponse login(UnifiedLoginRequest req) {
        // Normalizar ANTES de validar patrón
        String code  = req.getCode().trim().toUpperCase();
        String email = req.getEmail().trim().toLowerCase();

        log.debug("Login intent — code={} email={}", code, email);

        if (code.matches(ADMIN_CODE_PATTERN))
            return AuthResponse.builder().token(handleAdminLogin(email, req.getPassword(), code)).build();
        if (code.matches(STUDENT_CODE_PATTERN))
            return AuthResponse.builder().token(handleStudentLogin(email, req.getPassword(), code)).build();

        throw new ApiException(ErrorCode.INVALID_LOGIN_CODE);
    }

    private String handleAdminLogin(String email, String password, String code) {
        log.debug("handleAdminLogin — buscando email={} code={}", email, code);

        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("handleAdminLogin — email no encontrado: {}", email);
                    return new ApiException(ErrorCode.INVALID_CREDENTIALS);
                });

        log.debug("handleAdminLogin — admin encontrado: id={} active={} passwordHashLen={}",
                admin.getAdminId(), admin.getIsActive(), admin.getPasswordHash().length());

        if (!admin.getAdminId().equalsIgnoreCase(code)) {
            log.warn("handleAdminLogin — código no coincide: BD={} recibido={}",
                    admin.getAdminId(), code);
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!Boolean.TRUE.equals(admin.getIsActive())) {
            log.warn("handleAdminLogin — cuenta inactiva: {}", admin.getAdminId());
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            log.warn("handleAdminLogin — contraseña incorrecta para adminId={} hashLen={}",
                    admin.getAdminId(), admin.getPasswordHash().length());
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        admin.setLastLoginAt(LocalDateTime.now());
        adminRepository.save(admin);
        log.info("handleAdminLogin — login exitoso adminId={}", admin.getAdminId());
        return jwtService.generateAdminToken(admin.getAdminId(), admin.getEmail());
    }

    private String handleStudentLogin(String email, String password, String code) {
        // Buscar por studentId (código de 7 dígitos)
        Student student = studentRepository.findByStudentId(code)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));

        if (!student.getEmail().equalsIgnoreCase(email))
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        if (!Boolean.TRUE.equals(student.getIsActive()))
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
        if (!Boolean.TRUE.equals(student.getEmailVerified()))
            throw new ApiException(ErrorCode.EMAIL_NOT_VERIFIED);
        if (!passwordEncoder.matches(password, student.getPasswordHash()))
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);

        student.setLastLoginAt(LocalDateTime.now());
        studentRepository.save(student);
        return jwtService.generateStudentToken(
                student.getStudentId(), student.getProgramId(), student.getEmail());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  REGISTRO ESTUDIANTE
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public StudentDto registerStudent(StudentRegisterRequest req) {
        String studentId = req.getStudentId().trim().toUpperCase();
        String programId = studentId.substring(0, 3);
        String email     = req.getEmail().trim().toLowerCase();
        String phone     = req.getPhone().trim();

        if (!programRepository.existsById(programId))
            throw new ApiException(ErrorCode.PROGRAM_NOT_FOUND);
        if (studentRepository.existsByStudentId(studentId))
            throw new ApiException(ErrorCode.CODE_ALREADY_EXISTS);
        if (studentRepository.existsByEmail(email))
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        if (studentRepository.existsByPhone(phone))
            throw new ApiException(ErrorCode.PHONE_ALREADY_EXISTS);

        Student student = Student.builder()
                .studentId(studentId)
                .programId(programId)
                .firstName(req.getFirstName())
                .secondName(req.getSecondName())
                .firstSurname(req.getFirstSurname())
                .secondSurname(req.getSecondSurname())
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .build();
        studentRepository.save(student);

        String raw = generateAndSaveStudentToken(
                studentId, StudentTokenType.EMAIL_VERIFY, emailVerifyExpirationMs);
        emailService.sendVerificationEmail(email, student.getFirstName(), raw);

        return StudentDto.from(student);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  VERIFICACIÓN DE EMAIL
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public void verifyEmail(String rawToken) {
        String hash = TokenHashUtil.hash(rawToken.trim());
        log.debug("verifyEmail — hash={}", hash);

        StudentToken token = studentTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID));

        if (token.getTokenType() != StudentTokenType.EMAIL_VERIFY)
            throw new ApiException(ErrorCode.TOKEN_INVALID);
        if (token.isExpired())
            throw new ApiException(ErrorCode.TOKEN_INVALID);
        if (token.isUsed())
            throw new ApiException(ErrorCode.TOKEN_ALREADY_USED);

        Student student = studentRepository.findByStudentId(token.getStudentId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));

        if (Boolean.TRUE.equals(student.getEmailVerified()))
            throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "El correo ya fue verificado");

        student.setEmailVerified(true);
        studentRepository.save(student);

        token.setUsedAt(LocalDateTime.now());
        studentTokenRepository.save(token);

        emailService.sendWelcomeEmail(student.getEmail(), student.getFirstName());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  REENVÍO DE VERIFICACIÓN (con rate limit en backend)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public ResendVerificationResponse resendVerification(String rawEmail) {
        String email = rawEmail.trim().toLowerCase();

        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND,
                        "No existe una cuenta con ese correo."));

        if (Boolean.TRUE.equals(student.getEmailVerified()))
            throw new ApiException(ErrorCode.EMAIL_ALREADY_VERIFIED,
                    "Tu correo ya está verificado. Inicia sesión.");

        String studentId = student.getStudentId();

        // Tope por ventana: máximo N reenvíos por hora
        long lastHour = studentTokenRepository.countByStudentIdAndTokenTypeAndCreatedAtAfter(
                studentId, StudentTokenType.EMAIL_VERIFY, LocalDateTime.now().minusHours(1));
        if (lastHour >= RESEND_MAX_PER_HOUR)
            throw new ApiException(ErrorCode.RESEND_TOO_SOON,
                    "Alcanzaste el límite de reenvíos por hora. Intenta más tarde.");

        // Cooldown: tiempo mínimo entre reenvíos
        var last = studentTokenRepository
                .findTopByStudentIdAndTokenTypeOrderByCreatedAtDesc(studentId, StudentTokenType.EMAIL_VERIFY);
        if (last.isPresent()) {
            long elapsed = java.time.Duration.between(last.get().getCreatedAt(), LocalDateTime.now()).getSeconds();
            if (elapsed < RESEND_COOLDOWN_SECONDS) {
                long wait = RESEND_COOLDOWN_SECONDS - elapsed;
                throw new ApiException(ErrorCode.RESEND_TOO_SOON,
                        "Espera " + wait + " segundos antes de reenviar el correo.");
            }
        }

        // Invalidar tokens previos de verificación y emitir uno nuevo
        studentTokenRepository.invalidateAllByStudentAndType(studentId, StudentTokenType.EMAIL_VERIFY);
        String raw = generateAndSaveStudentToken(studentId, StudentTokenType.EMAIL_VERIFY, emailVerifyExpirationMs);
        emailService.sendVerificationEmail(email, student.getFirstName(), raw);
        log.info("resendVerification — correo de verificación reenviado a {}", email);

        return new ResendVerificationResponse(RESEND_COOLDOWN_SECONDS);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FORGOT PASSWORD UNIFICADO
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public void forgotPassword(UnifiedForgotPasswordRequest req) {
        // Normalizar inputs
        String code  = req.getCode().trim().toUpperCase();
        String email = req.getEmail().trim().toLowerCase();
        String phone = req.getPhone().trim();

        log.debug("forgotPassword — code={} email={}", code, email);

        if (code.matches(ADMIN_CODE_PATTERN)) {
            handleAdminForgotPassword(code, email, phone);
            return;
        }
        if (code.matches(STUDENT_CODE_PATTERN)) {
            handleStudentForgotPassword(code, email, phone);
            return;
        }
        // Respuesta genérica — no revelar motivo
        log.debug("forgotPassword — código con formato no reconocido: {}", code);
    }

    private void handleAdminForgotPassword(String adminId, String email, String phone) {
        adminRepository.findById(adminId).ifPresent(admin -> {
            if (!admin.getEmail().equalsIgnoreCase(email)) {
                log.debug("forgotPassword admin — email no coincide");
                return;
            }
            if (!admin.getPhone().equals(phone)) {
                log.debug("forgotPassword admin — teléfono no coincide");
                return;
            }
            if (!Boolean.TRUE.equals(admin.getIsActive())) {
                log.debug("forgotPassword admin — cuenta inactiva");
                return;
            }

            // Invalidar tokens anteriores
            adminTokenRepository.invalidateAllByAdminAndType(adminId, AdminTokenType.PASSWORD_RESET);

            String raw = generateAndSaveAdminToken(
                    adminId, AdminTokenType.PASSWORD_RESET, passwordResetExpirationMs);
            emailService.sendPasswordResetEmail(admin.getEmail(), admin.getFirstName(), raw);
            log.info("forgotPassword admin — enlace enviado a {}", admin.getEmail());
        });
    }

    private void handleStudentForgotPassword(String studentId, String email, String phone) {
        studentRepository.findByStudentId(studentId).ifPresent(student -> {
            if (!student.getEmail().equalsIgnoreCase(email)) {
                log.debug("forgotPassword student — email no coincide");
                return;
            }
            if (!student.getPhone().equals(phone)) {
                log.debug("forgotPassword student — teléfono no coincide");
                return;
            }
            if (!Boolean.TRUE.equals(student.getIsActive())) {
                log.debug("forgotPassword student — cuenta inactiva");
                return;
            }

            // Invalidar tokens anteriores
            studentTokenRepository.invalidateAllByStudentAndType(studentId, StudentTokenType.PASSWORD_RESET);

            String raw = generateAndSaveStudentToken(
                    studentId, StudentTokenType.PASSWORD_RESET, passwordResetExpirationMs);
            emailService.sendPasswordResetEmail(student.getEmail(), student.getFirstName(), raw);
            log.info("forgotPassword student — enlace enviado a {}", student.getEmail());
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  RESET PASSWORD UNIFICADO
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        // Limpiar el token por si viene con espacios o caracteres extra
        String rawToken = req.getToken().trim();
        String hash     = TokenHashUtil.hash(rawToken);

        log.debug("resetPassword — token_len={} hash={}", rawToken.length(), hash);

        // Intentar como token de estudiante primero
        var studentTokenOpt = studentTokenRepository.findByTokenHash(hash);
        if (studentTokenOpt.isPresent()) {
            applyStudentReset(studentTokenOpt.get(), req.getNewPassword());
            return;
        }

        // Intentar como token de admin
        var adminTokenOpt = adminTokenRepository.findByTokenHash(hash);
        if (adminTokenOpt.isPresent()) {
            applyAdminReset(adminTokenOpt.get(), req.getNewPassword());
            return;
        }

        log.warn("resetPassword — token no encontrado en ninguna tabla. hash={}", hash);
        throw new ApiException(ErrorCode.TOKEN_INVALID);
    }

    private void applyStudentReset(StudentToken token, String newPassword) {
        log.debug("applyStudentReset — studentId={} type={} expired={} used={}",
                token.getStudentId(), token.getTokenType(), token.isExpired(), token.isUsed());

        if (token.getTokenType() != StudentTokenType.PASSWORD_RESET)
            throw new ApiException(ErrorCode.TOKEN_INVALID);
        if (token.isExpired())
            throw new ApiException(ErrorCode.TOKEN_INVALID);
        if (token.isUsed())
            throw new ApiException(ErrorCode.TOKEN_ALREADY_USED);

        Student student = studentRepository.findByStudentId(token.getStudentId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));

        student.setPasswordHash(passwordEncoder.encode(newPassword));
        studentRepository.save(student);

        // Marcar token como usado
        token.setUsedAt(LocalDateTime.now());
        studentTokenRepository.save(token);

        log.info("applyStudentReset — contraseña actualizada para studentId={}", student.getStudentId());
    }

    private void applyAdminReset(AdminToken token, String newPassword) {
        log.debug("applyAdminReset — adminId={} type={} expired={} used={}",
                token.getAdminId(), token.getTokenType(), token.isExpired(), token.isUsed());

        if (token.getTokenType() != AdminTokenType.PASSWORD_RESET)
            throw new ApiException(ErrorCode.TOKEN_INVALID);
        if (token.isExpired())
            throw new ApiException(ErrorCode.TOKEN_INVALID);
        if (token.isUsed())
            throw new ApiException(ErrorCode.TOKEN_ALREADY_USED);

        Admin admin = adminRepository.findById(token.getAdminId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));

        admin.setPasswordHash(passwordEncoder.encode(newPassword));
        adminRepository.save(admin);

        // Marcar token como usado
        token.setUsedAt(LocalDateTime.now());
        adminTokenRepository.save(token);

        log.info("applyAdminReset — contraseña actualizada para adminId={}", admin.getAdminId());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HELPERS PRIVADOS
    // ═══════════════════════════════════════════════════════════════════════

    private String generateAndSaveStudentToken(String studentId, StudentTokenType type, long ms) {
        String raw  = TokenHashUtil.generate();
        String hash = TokenHashUtil.hash(raw);
        log.debug("generateStudentToken — studentId={} type={} hash={}", studentId, type, hash);

        studentTokenRepository.save(StudentToken.builder()
                .studentId(studentId)
                .tokenHash(hash)
                .tokenType(type)
                .expiresAt(LocalDateTime.now().plusSeconds(ms / 1000))
                .build());
        return raw;
    }

    private String generateAndSaveAdminToken(String adminId, AdminTokenType type, long ms) {
        String raw  = TokenHashUtil.generate();
        String hash = TokenHashUtil.hash(raw);
        log.debug("generateAdminToken — adminId={} type={} hash={}", adminId, type, hash);

        adminTokenRepository.save(AdminToken.builder()
                .adminId(adminId)
                .tokenHash(hash)
                .tokenType(type)
                .expiresAt(LocalDateTime.now().plusSeconds(ms / 1000))
                .build());
        return raw;
    }
}