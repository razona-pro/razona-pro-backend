package com.razonapro.razonaprobackend.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class AuthResponse {
    private String token;
}