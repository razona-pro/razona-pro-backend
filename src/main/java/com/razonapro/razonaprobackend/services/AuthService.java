package com.razonapro.razonaprobackend.services;

import com.razonapro.razonaprobackend.dtos.request.LoginRequest;
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
import com.razonapro.razonaprobackend.util.IdGenerator;
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

    // ── Admin login ───────────────────────────────────────────
    @Transactional
    public AuthResponse adminLogin(LoginRequest req) {
        Admin admin = adminRepository.findByEmail(req.getEmail())
            .orElseThrow(() -> new ApiException("Credenciales incorrectas", HttpStatus.UNAUTHORIZED));

        if (!admin.getIsActive())
            throw new ApiException("Cuenta de administrador deshabilitada", HttpStatus.FORBIDDEN);

        if (!passwordEncoder.matches(req.getPassword(), admin.getPasswordHash()))
            throw new ApiException("Credenciales incorrectas", HttpStatus.UNAUTHORIZED);

        admin.setLastLoginAt(LocalDateTime.now());
        adminRepository.save(admin);

        String token = jwtService.generateAdminToken(admin.getAdminId(), admin.getEmail());
        return AuthResponse.builder()
            .token(token)
            .userType("ADMIN")
            .id(admin.getAdminId())
            .email(admin.getEmail())
            .firstName(admin.getFirstName())
            .firstSurname(admin.getFirstSurname())
            .build();
    }

    // ── Student login ─────────────────────────────────────────
    @Transactional
    public AuthResponse studentLogin(LoginRequest req) {
        Student student = studentRepository.findByEmail(req.getEmail())
            .orElseThrow(() -> new ApiException("Credenciales incorrectas", HttpStatus.UNAUTHORIZED));

        if (!student.getIsActive())
            throw new ApiException("Cuenta deshabilitada", HttpStatus.FORBIDDEN);

        if (!student.getEmailVerified())
            throw new ApiException("Debes verificar tu correo electrónico antes de iniciar sesión", HttpStatus.FORBIDDEN);

        if (!passwordEncoder.matches(req.getPassword(), student.getPasswordHash()))
            throw new ApiException("Credenciales incorrectas", HttpStatus.UNAUTHORIZED);

        student.setLastLoginAt(LocalDateTime.now());
        studentRepository.save(student);

        String token = jwtService.generateStudentToken(
            student.getStudentId(), student.getProgramId(), student.getEmail());

        return AuthResponse.builder()
            .token(token)
            .userType("STUDENT")
            .id(student.getStudentId())
            .programId(student.getProgramId())
            .email(student.getEmail())
            .firstName(student.getFirstName())
            .firstSurname(student.getFirstSurname())
            .build();
    }

    // ── Student register ──────────────────────────────────────
    @Transactional
    public StudentDto registerStudent(StudentRegisterRequest req) {
        if (studentRepository.existsByEmail(req.getEmail()))
            throw new ApiException("El email ya está registrado");

        if (studentRepository.existsByPhone(req.getPhone()))
            throw new ApiException("El teléfono ya está registrado");

        if (!programRepository.existsById(req.getProgramId()))
            throw new ApiException("Programa no encontrado");

        long count = studentRepository.countAll();
        String studentId = IdGenerator.studentId(count);

        Student student = Student.builder()
            .studentId(studentId)
            .programId(req.getProgramId())
            .firstName(req.getFirstName())
            .secondName(req.getSecondName())
            .firstSurname(req.getFirstSurname())
            .secondSurname(req.getSecondSurname())
            .email(req.getEmail())
            .phone(req.getPhone())
            .passwordHash(passwordEncoder.encode(req.getPassword()))
            .build();

        studentRepository.save(student);

        // Enviar email de verificación de forma asíncrona
        String verifyToken = jwtService.generateEmailVerificationToken(
            studentId, req.getProgramId(), req.getEmail());
        emailService.sendVerificationEmail(req.getEmail(), req.getFirstName(), verifyToken);

        return StudentDto.from(student);
    }

    // ── Verificar email ───────────────────────────────────────
    @Transactional
    public void verifyEmail(String token) {
        if (!jwtService.isValid(token))
            throw new ApiException("Token de verificación inválido o expirado", HttpStatus.BAD_REQUEST);

        Claims claims = jwtService.parseToken(token);
        String purpose = claims.get("purpose", String.class);
        if (!"EMAIL_VERIFY".equals(purpose))
            throw new ApiException("Token inválido", HttpStatus.BAD_REQUEST);

        String studentId = claims.getSubject();
        String programId = claims.get("programId", String.class);
        String email     = claims.get("email", String.class);

        Student student = studentRepository.findByEmail(email)
            .orElseThrow(() -> new ApiException("Estudiante no encontrado"));

        if (student.getEmailVerified())
            throw new ApiException("El correo ya fue verificado anteriormente");

        student.setEmailVerified(true);
        studentRepository.save(student);

        emailService.sendWelcomeEmail(email, student.getFirstName());
    }
}
