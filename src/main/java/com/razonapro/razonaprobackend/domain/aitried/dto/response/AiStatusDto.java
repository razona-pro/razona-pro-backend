// src/main/java/com/razonapro/razonaprobackend/domain/aitried/dto/response/AiStatusDto.java
package com.razonapro.razonaprobackend.domain.aitried.dto.response;

public record AiStatusDto(
        boolean enabled,
        String  provider,
        String  model,
        boolean reachable,
        String  message
) {}