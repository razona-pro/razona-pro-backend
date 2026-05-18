package com.razonapro.razonaprobackend.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.razonapro.razonaprobackend.models.Student;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StudentDto {
    private String studentId;
    private String programId;
    private String programName;
    private String firstName;
    private String secondName;
    private String firstSurname;
    private String secondSurname;
    private String email;
    private String phone;
    private Boolean isActive;
    private Boolean emailVerified;
    private Boolean identityVerified;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public static StudentDto from(Student s) {
        return StudentDto.builder()
            .studentId(s.getStudentId())
            .programId(s.getProgramId())
            .programName(s.getProgram() != null ? s.getProgram().getProgramName() : null)
            .firstName(s.getFirstName())
            .secondName(s.getSecondName())
            .firstSurname(s.getFirstSurname())
            .secondSurname(s.getSecondSurname())
            .email(s.getEmail())
            .phone(s.getPhone())
            .isActive(s.getIsActive())
            .emailVerified(s.getEmailVerified())
            .identityVerified(s.getIdentityVerified())
            .createdAt(s.getCreatedAt())
            .lastLoginAt(s.getLastLoginAt())
            .build();
    }
}
