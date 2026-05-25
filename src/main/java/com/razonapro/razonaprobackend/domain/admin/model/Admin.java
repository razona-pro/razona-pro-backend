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
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
