package com.razonapro.razonaprobackend.domain.admin.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.razonapro.razonaprobackend.domain.admin.model.Admin;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminDto {
    private String adminId;
    private String firstName;
    private String secondName;
    private String firstSurname;
    private String secondSurname;
    private String email;
    private String phone;
    private Boolean isActive;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;

    public static AdminDto from(Admin a) {
        return AdminDto.builder()
                .adminId(a.getAdminId())
                .firstName(a.getFirstName())
                .secondName(a.getSecondName())
                .firstSurname(a.getFirstSurname())
                .secondSurname(a.getSecondSurname())
                .email(a.getEmail())
                .phone(a.getPhone())
                .isActive(a.getIsActive())
                .lastLoginAt(a.getLastLoginAt())
                .createdAt(a.getCreatedAt())
                .build();
    }
}