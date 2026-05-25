package com.razonapro.razonaprobackend.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String token;
    private String userType;  // ADMIN | STUDENT
    private String id;
    private String programId;
    private String email;
    private String firstName;
    private String firstSurname;
}
