package com.razonapro.razonaprobackend.domain.admin.model;

import com.razonapro.razonaprobackend.infrastructure.util.BooleanToYNConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "admins", schema = "razonapro")
public class Admin {

    @Id
    @Column(name = "admin_id", length = 6)
    private String adminId;

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

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
        if (adminId      != null) adminId      = adminId.trim().toUpperCase();
        if (firstName    != null) firstName    = firstName.trim().toUpperCase();
        if (secondName   != null) secondName   = secondName.trim().toUpperCase();
        if (firstSurname != null) firstSurname = firstSurname.trim().toUpperCase();
        if (secondSurname != null) secondSurname = secondSurname.trim().toUpperCase();
        if (email        != null) email        = email.trim().toUpperCase();
        if (phone        != null) phone        = phone.trim();
    }
}