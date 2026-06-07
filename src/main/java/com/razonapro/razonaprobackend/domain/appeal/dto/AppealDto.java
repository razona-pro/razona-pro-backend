package com.razonapro.razonaprobackend.domain.appeal.dto;

import com.razonapro.razonaprobackend.domain.appeal.model.Appeal;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @Builder
public class AppealDto {
    private String        appealId;
    private String        studentId;
    private String        programId;
    private String        studentName;
    private String        deactivationReason;
    private String        message;
    private String        status;
    private String        adminResponse;
    private String        resolvedBy;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public static AppealDto from(Appeal a) {
        return from(a, null);
    }

    public static AppealDto from(Appeal a, String studentName) {
        return AppealDto.builder()
                .appealId(a.getAppealId())
                .studentId(a.getStudentId())
                .programId(a.getProgramId())
                .studentName(studentName)
                .deactivationReason(a.getDeactivationReason())
                .message(a.getMessage())
                .status(a.getStatus())
                .adminResponse(a.getAdminResponse())
                .resolvedBy(a.getResolvedBy())
                .createdAt(a.getCreatedAt())
                .resolvedAt(a.getResolvedAt())
                .build();
    }
}
