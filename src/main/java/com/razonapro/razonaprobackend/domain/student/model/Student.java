package com.razonapro.razonaprobackend.domain.student.model;

import com.razonapro.razonaprobackend.infrastructure.util.BooleanToYNConverter;
import com.razonapro.razonaprobackend.models.ids.StudentId;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "students", schema = "razonapro")
@IdClass(StudentId.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Student {

    @Id
    @Column(name = "student_id", length = 7, nullable = false)
    private String studentId;

    @Id
    @Column(name = "program_id", length = 3, nullable = false)
    private String programId;

    @Column(name = "first_name", length = 15, nullable = false)
    private String firstName;

    @Column(name = "second_name", length = 15)
    private String secondName;

    @Column(name = "first_surname", length = 15, nullable = false)
    private String firstSurname;

    @Column(name = "second_surname", length = 15)
    private String secondSurname;

    @Column(name = "email", length = 50, nullable = false, unique = true)
    private String email;

    @Column(name = "phone", length = 15, nullable = false, unique = true)
    private String phone;

    @Column(name = "password_hash", length = 60, nullable = false)
    private String passwordHash;

    @Convert(converter = BooleanToYNConverter.class)
    @Column(name = "email_verified", nullable = false, length = 1)
    @Builder.Default
    private Boolean emailVerified = false;

    @Convert(converter = BooleanToYNConverter.class)
    @Column(name = "is_active", nullable = false, length = 1)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Lifecycle hooks ───────────────────────────────────────

    @PrePersist
    private void onInsert() {
        normalizeFields();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void onUpdate() {
        normalizeFields();
        updatedAt = LocalDateTime.now();
    }

    private void normalizeFields() {
        if (studentId     != null) studentId     = studentId.trim().toUpperCase();
        if (programId     != null) programId     = programId.trim().toUpperCase();
        if (firstName     != null) firstName     = firstName.trim().toUpperCase();
        if (secondName    != null) secondName    = secondName.trim().toUpperCase();
        if (firstSurname  != null) firstSurname  = firstSurname.trim().toUpperCase();
        if (secondSurname != null) secondSurname = secondSurname.trim().toUpperCase();
        if (email         != null) email         = email.trim().toUpperCase();
        if (phone         != null) phone         = phone.trim();
        // passwordHash → NUNCA toUpperCase, bcrypt es case-sensitive
    }
}