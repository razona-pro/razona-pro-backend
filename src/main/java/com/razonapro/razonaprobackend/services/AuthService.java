package com.razonapro.razonaprobackend.services;

import com.razonapro.razonaprobackend.dtos.request.UnifiedLoginRequest;
import com.razonapro.razonaprobackend.dtos.request.StudentRegisterRequest;
import com.razonapro.razonaprobackend.dtos.response.AuthResponse;
import com.razonapro.razonaprobackend.dtos.response.StudentDto;
import com.razonapro.razonaprobackend.exception.ApiException;
import com.razonapro.razonaprobackend.models.Admin;
import com.razonapro.razonaprobackend.models.Student;
import com.razonapro.razonaprobackend.repositories.AdminRepository;
import com.razonapro.razonaprobackend.repositories.ProgramRepository;
import com.razonapro.razonaprobackend.repositories.StudentRepository;
import com.razonapro.razonaprobackend.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdminRepository   adminRepository;
    private final StudentRepository studentRepository;
    private final ProgramRepository programRepository;
    private final JwtService        jwtService;
    private final PasswordEncoder   passwordEncoder;
    private final EmailService      emailService;

    // ── Login unificado ───────────────────────────────────────
    @Transactional
    public AuthResponse login(UnifiedLoginRequest req) {
        String code  = req.getCode().trim().toUpperCase();
        String email = req.getEmail().trim().toUpperCase();

        // Admin: código tipo AMN001 (letras + números, 6 chars)
        if (code.matches("^[A-Z]{3}[0-9]{3}$")) {
            return handleAdminLogin(email, req.getPassword(), code);
        }

        // Estudiante: código de 7 dígitos tipo 0192250
        if (code.matches("^[0-9]{7}$")) {
            return handleStudentLogin(email, req.getPassword(), code);
        }

        throw new ApiException("Código inválido. Debe ser un código de administrador (ej: AMN001) o de estudiante (ej: 0192250)", HttpStatus.UNAUTHORIZED);
    }

    private AuthResponse handleAdminLogin(String email, String password, String code) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Credenciales incorrectas", HttpStatus.UNAUTHORIZED));

        if (!admin.getAdminId().equalsIgnoreCase(code))
            throw new ApiException("Credenciales incorrectas", HttpStatus.UNAUTHORIZED);

        if (!admin.getIsActive())
            throw new ApiException("Cuenta de administrador deshabilitada", HttpStatus.FORBIDDEN);

        if (!passwordEncoder.matches(password, admin.getPasswordHash()))
            throw new ApiException("Credenciales incorrectas", HttpStatus.UNAUTHORIZED);

        admin.setLastLoginAt(LocalDateTime.now());
        adminRepository.save(admin);

        return AuthResponse.builder()
                .token(jwtService.generateAdminToken(admin.getAdminId(), admin.getEmail()))
                .userType("ADMIN")
                .id(admin.getAdminId())
                .email(admin.getEmail())
                .firstName(admin.getFirstName())
                .firstSurname(admin.getFirstSurname())
                .build();
    }

    private AuthResponse handleStudentLogin(String email, String password, String code) {
        Student student = studentRepository.findByStudentId(code)
                .orElseThrow(() -> new ApiException("Credenciales incorrectas", HttpStatus.UNAUTHORIZED));

        if (!student.getEmail().equalsIgnoreCase(email))
            throw new ApiException("Credenciales incorrectas", HttpStatus.UNAUTHORIZED);

        if (!student.getIsActive())
            throw new ApiException("Cuenta deshabilitada", HttpStatus.FORBIDDEN);

        if (!student.getEmailVerified())
            throw new ApiException("Debes verificar tu correo electrónico antes de iniciar sesión", HttpStatus.FORBIDDEN);

        if (!passwordEncoder.matches(password, student.getPasswordHash()))
            throw new ApiException("Credenciales incorrectas", HttpStatus.UNAUTHORIZED);

        student.setLastLoginAt(LocalDateTime.now());
        studentRepository.save(student);

        return AuthResponse.builder()
                .token(jwtService.generateStudentToken(
                        student.getStudentId(), student.getProgramId(), student.getEmail()))
                .userType("STUDENT")
                .id(student.getStudentId())
                .programId(student.getProgramId())
                .email(student.getEmail())
                .firstName(student.getFirstName())
                .firstSurname(student.getFirstSurname())
                .build();
    }

    // ── Registro estudiante ───────────────────────────────────
    @Transactional
    public StudentDto registerStudent(StudentRegisterRequest req) {
        String studentId = req.getStudentId().trim();
        String programId = studentId.substring(0, 3); // extrae 019 de 0192250
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

        String verifyToken = jwtService.generateEmailVerificationToken(studentId, programId, email);
        emailService.sendVerificationEmail(email, student.getFirstName(), verifyToken);

        return StudentDto.from(student);
    }

    // ── Verificar email ───────────────────────────────────────
    @Transactional
    public void verifyEmail(String token) {
        if (!jwtService.isValid(token))
            throw new ApiException("Token de verificación inválido o expirado", HttpStatus.BAD_REQUEST);

        Claims claims  = jwtService.parseToken(token);
        String purpose = claims.get("purpose", String.class);
        if (!"EMAIL_VERIFY".equals(purpose))
            throw new ApiException("Token inválido", HttpStatus.BAD_REQUEST);

        String email = claims.get("email", String.class);

        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Estudiante no encontrado"));

        if (student.getEmailVerified())
            throw new ApiException("El correo ya fue verificado anteriormente");

        student.setEmailVerified(true);
        studentRepository.save(student);

        emailService.sendWelcomeEmail(email, student.getFirstName());
    }
}