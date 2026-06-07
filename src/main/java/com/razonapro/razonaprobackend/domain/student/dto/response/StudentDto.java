package com.razonapro.razonaprobackend.domain.student.dto.response;

import com.razonapro.razonaprobackend.domain.student.model.Student;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class StudentDto {
    private String studentId;
    private String programId;
    private String firstName;
    private String secondName;
    private String firstSurname;
    private String secondSurname;
    private String email;
    private String phone;
    private Boolean emailVerified;
    private Boolean identityVerified;
    private Boolean isActive;
    private String  deactivationReason;

    public static StudentDto from(Student s) {
        return StudentDto.builder()
                .studentId(s.getStudentId())
                .programId(s.getProgramId())
                .firstName(s.getFirstName())
                .secondName(s.getSecondName())
                .firstSurname(s.getFirstSurname())
                .secondSurname(s.getSecondSurname())
                .email(s.getEmail())
                .phone(s.getPhone())
                .emailVerified(s.getEmailVerified())
                .identityVerified(s.getIdentityVerified())
                .isActive(s.getIsActive())
                .deactivationReason(s.getDeactivationReason())
                .build();
    }
}