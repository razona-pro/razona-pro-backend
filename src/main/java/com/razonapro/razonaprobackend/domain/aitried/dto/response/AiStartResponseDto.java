// domain/aitried/dto/response/AiStartResponseDto.java
package com.razonapro.razonaprobackend.domain.aitried.dto.response;

public record AiStartResponseDto(AiTriedDto tried, AiQuestionDto firstQuestion, int totalGenerated) {}