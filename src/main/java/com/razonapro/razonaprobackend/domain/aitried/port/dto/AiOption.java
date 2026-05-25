package com.razonapro.razonaprobackend.domain.aitried.port.dto;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class AiOption {
    private String text;
    private boolean isCorrect;
}