package com.razonapro.razonaprobackend.models;

import com.razonapro.razonaprobackend.models.ids.StudentId;
import com.razonapro.razonaprobackend.util.BooleanToYNConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "students", schema = "razonapro")
@IdClass(StudentId.class)
public class Student {

    @Id
    @Column(name = "student_id", length = 7)
    private String studentId;

    @Id
    @Column(name = "program_id", length = 3)
    private String programId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", insertable = false, updatable = false)
    private Program program;

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

    @Column(name = "password_hash", length = 72, nullable = false)
    private String passwordHash;

    @Convert(converter = BooleanToYNConverter.class)
    @Column(name = "is_active", columnDefinition = "CHAR(1)", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Convert(converter = BooleanToYNConverter.class)
    @Column(name = "email_verified", columnDefinition = "CHAR(1)", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Convert(converter = BooleanToYNConverter.class)
    @Column(name = "identity_verified", columnDefinition = "CHAR(1)", nullable = false)
    @Builder.Default
    private Boolean identityVerified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
