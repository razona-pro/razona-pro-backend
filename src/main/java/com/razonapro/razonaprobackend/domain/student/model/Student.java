package com.razonapro.razonaprobackend.domain.student.model;

import com.razonapro.razonaprobackend.shared.ids.StudentId;
import com.razonapro.razonaprobackend.shared.jpa.Normalizable;
import com.razonapro.razonaprobackend.shared.jpa.NormalizingEntityListener;
import com.razonapro.razonaprobackend.shared.util.StringNormalizer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@IdClass(StudentId.class)
@EntityListeners(NormalizingEntityListener.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Student implements Normalizable {

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

    @Column(name = "is_active", nullable = false, length = 1)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "email_verified", nullable = false, length = 1)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "identity_verified", nullable = false, length = 1)
    @Builder.Default
    private Boolean identityVerified = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onInsert() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public void normalize() {
        studentId     = StringNormalizer.upper(studentId);
        programId     = StringNormalizer.trim(programId);
        firstName     = StringNormalizer.upper(firstName);
        secondName    = StringNormalizer.upper(secondName);
        firstSurname  = StringNormalizer.upper(firstSurname);
        secondSurname = StringNormalizer.upper(secondSurname);
        email         = StringNormalizer.upper(email);
        phone         = StringNormalizer.trim(phone);
    }
}