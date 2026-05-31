// src/main/java/com/razonapro/razonaprobackend/domain/aitried/port/dto/AiHintContext.java
package com.razonapro.razonaprobackend.domain.aitried.port.dto;

import java.util.List;

public record AiHintContext(
        String competenceName,
        String questionStatement,
        List<String> optionTexts,
        String correctOptionText,   // el servidor lo sabe; el prompt instruye al modelo a NO revelarlo en nivel 1-2
        int    hintLevel            // 1, 2 o 3
) {}