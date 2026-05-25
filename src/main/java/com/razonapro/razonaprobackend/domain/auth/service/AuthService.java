package com.razonapro.razonaprobackend.domain.auth.service;

import com.razonapro.razonaprobackend.domain.admin.model.Admin;
import com.razonapro.razonaprobackend.domain.admin.repository.AdminRepository;
import com.razonapro.razonaprobackend.domain.auth.dto.request.*;
import com.razonapro.razonaprobackend.domain.auth.dto.response.AuthResponse;
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

    private static final String ADMIN_CODE_PATTERN   = "^[A-Z]{3}[0-9]{3}$";
    private static final String STUDENT_CODE_PATTERN = "^[0-9]{7}$";

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

    // ═══════════════════════════════════════════════════════════════════════
    //  LOGIN UNIFICADO
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public AuthResponse login(UnifiedLoginRequest req) {
        String code  = StringNormalizer.upper(req.getCode());
        String email = StringNormalizer.upper(req.getEmail());

        if (code.matches(ADMIN_CODE_PATTERN))
            return AuthResponse.builder().token(handleAdminLogin(email, req.getPassword(), code)).build();
        if (code.matches(STUDENT_CODE_PATTERN))
            return AuthResponse.builder().token(handleStudentLogin(email, req.getPassword(), code)).build();

        throw new ApiException(ErrorCode.INVALID_LOGIN_CODE);
    }

    private String handleAdminLogin(String email, String password, String code) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));
        if (!admin.getAdminId().equalsIgnoreCase(code))
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        if (!Boolean.TRUE.equals(admin.getIsActive()))
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
        if (!passwordEncoder.matches(password, admin.getPasswordHash()))
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);

        admin.setLastLoginAt(LocalDateTime.now());
        adminRepository.save(admin);
        return jwtService.generateAdminToken(admin.getAdminId(), admin.getEmail());
    }

    private String handleStudentLogin(String email, String password, String code) {
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
    //  REGISTRO ESTUDIANTE (público)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public StudentDto registerStudent(StudentRegisterRequest req) {
        String studentId = StringNormalizer.upper(req.getStudentId());
        String programId = studentId.substring(0, 3);
        String email     = StringNormalizer.upper(req.getEmail());
        String phone     = StringNormalizer.trim(req.getPhone());

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
        StudentToken token = studentTokenRepository.findByTokenHash(TokenHashUtil.hash(rawToken))
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID));

        if (token.getTokenType() != StudentTokenType.EMAIL_VERIFY)
            throw new ApiException(ErrorCode.TOKEN_INVALID);
        if (!token.isValid())
            throw new ApiException(ErrorCode.TOKEN_INVALID);

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
    //  FORGOT PASSWORD UNIFICADO (admin + estudiante)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public void forgotPassword(UnifiedForgotPasswordRequest req) {
        String code  = StringNormalizer.upper(req.getCode());
        String email = StringNormalizer.upper(req.getEmail());
        String phone = StringNormalizer.trim(req.getPhone());

        if (code.matches(ADMIN_CODE_PATTERN)) {
            handleAdminForgotPassword(code, email, phone);
            return;
        }
        if (code.matches(STUDENT_CODE_PATTERN)) {
            handleStudentForgotPassword(code, email, phone);
            return;
        }
        // No revelamos el motivo: respuesta genérica fuera
        log.debug("forgotPassword: código con formato inválido");
    }

    private void handleAdminForgotPassword(String adminId, String email, String phone) {
        adminRepository.findById(adminId).ifPresent(admin -> {
            if (!admin.getEmail().equalsIgnoreCase(email))   return;
            if (!admin.getPhone().equals(phone))             return;
            if (!Boolean.TRUE.equals(admin.getIsActive()))   return;

            adminTokenRepository.invalidateAllByAdminAndType(adminId, AdminTokenType.PASSWORD_RESET);
            String raw = generateAndSaveAdminToken(
                    adminId, AdminTokenType.PASSWORD_RESET, passwordResetExpirationMs);
            emailService.sendPasswordResetEmail(admin.getEmail(), admin.getFirstName(), raw);
        });
    }

    private void handleStudentForgotPassword(String studentId, String email, String phone) {
        studentRepository.findByStudentId(studentId).ifPresent(student -> {
            if (!student.getEmail().equalsIgnoreCase(email)) return;
            if (!student.getPhone().equals(phone))           return;
            if (!Boolean.TRUE.equals(student.getIsActive())) return;

            studentTokenRepository.invalidateAllByStudentAndType(studentId, StudentTokenType.PASSWORD_RESET);
            String raw = generateAndSaveStudentToken(
                    studentId, StudentTokenType.PASSWORD_RESET, passwordResetExpirationMs);
            emailService.sendPasswordResetEmail(student.getEmail(), student.getFirstName(), raw);
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  RESET PASSWORD UNIFICADO (resuelve por token)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        String hash = TokenHashUtil.hash(req.getToken());

        // Probar primero como token de estudiante
        var studentTokenOpt = studentTokenRepository.findByTokenHash(hash);
        if (studentTokenOpt.isPresent()) {
            applyStudentReset(studentTokenOpt.get(), req.getNewPassword());
            return;
        }

        // Si no, probar como token de admin
        var adminTokenOpt = adminTokenRepository.findByTokenHash(hash);
        if (adminTokenOpt.isPresent()) {
            applyAdminReset(adminTokenOpt.get(), req.getNewPassword());
            return;
        }

        throw new ApiException(ErrorCode.TOKEN_INVALID);
    }

    private void applyStudentReset(StudentToken token, String newPassword) {
        if (token.getTokenType() != StudentTokenType.PASSWORD_RESET || !token.isValid())
            throw new ApiException(ErrorCode.TOKEN_INVALID);

        Student student = studentRepository.findByStudentId(token.getStudentId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));

        student.setPasswordHash(passwordEncoder.encode(newPassword));
        studentRepository.save(student);

        token.setUsedAt(LocalDateTime.now());
        studentTokenRepository.save(token);
        studentTokenRepository.invalidateAllByStudentAndType(
                student.getStudentId(), StudentTokenType.PASSWORD_RESET);
    }

    private void applyAdminReset(AdminToken token, String newPassword) {
        if (token.getTokenType() != AdminTokenType.PASSWORD_RESET || !token.isValid())
            throw new ApiException(ErrorCode.TOKEN_INVALID);

        Admin admin = adminRepository.findById(token.getAdminId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));

        admin.setPasswordHash(passwordEncoder.encode(newPassword));
        adminRepository.save(admin);

        token.setUsedAt(LocalDateTime.now());
        adminTokenRepository.save(token);
        adminTokenRepository.invalidateAllByAdminAndType(
                admin.getAdminId(), AdminTokenType.PASSWORD_RESET);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private String generateAndSaveStudentToken(String studentId, StudentTokenType type, long ms) {
        String raw = TokenHashUtil.generate();
        studentTokenRepository.save(StudentToken.builder()
                .studentId(studentId)
                .tokenHash(TokenHashUtil.hash(raw))
                .tokenType(type)
                .expiresAt(LocalDateTime.now().plusSeconds(ms / 1000))
                .build());
        return raw;
    }

    private String generateAndSaveAdminToken(String adminId, AdminTokenType type, long ms) {
        String raw = TokenHashUtil.generate();
        adminTokenRepository.save(AdminToken.builder()
                .adminId(adminId)
                .tokenHash(TokenHashUtil.hash(raw))
                .tokenType(type)
                .expiresAt(LocalDateTime.now().plusSeconds(ms / 1000))
                .build());
        return raw;
    }
}