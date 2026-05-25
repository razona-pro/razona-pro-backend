package com.razonapro.razonaprobackend.domain.admin.model;

import com.razonapro.razonaprobackend.shared.jpa.Normalizable;
import com.razonapro.razonaprobackend.shared.jpa.NormalizingEntityListener;
import com.razonapro.razonaprobackend.shared.util.StringNormalizer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "admins")
@EntityListeners(NormalizingEntityListener.class)
public class Admin implements Normalizable {

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

    @Column(name = "password_hash", length = 60, nullable = false)
    private String passwordHash;

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
        adminId       = StringNormalizer.upper(adminId);
        firstName     = StringNormalizer.upper(firstName);
        secondName    = StringNormalizer.upper(secondName);
        firstSurname  = StringNormalizer.upper(firstSurname);
        secondSurname = StringNormalizer.upper(secondSurname);
        email         = StringNormalizer.upper(email);
        phone         = StringNormalizer.trim(phone);
    }
}