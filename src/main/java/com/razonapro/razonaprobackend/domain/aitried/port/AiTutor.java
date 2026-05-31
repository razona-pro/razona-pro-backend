// src/main/java/com/razonapro/razonaprobackend/domain/aitried/port/AiTutor.java
package com.razonapro.razonaprobackend.domain.aitried.port;

import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiHintContext;

public interface AiTutor {
    String generateHint(AiHintContext context);
    boolean isAvailable();
}