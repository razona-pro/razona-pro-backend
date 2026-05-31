// src/main/java/com/razonapro/razonaprobackend/domain/aitried/port/AiQuestionGenerator.java
package com.razonapro.razonaprobackend.domain.aitried.port;

import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiGeneratedQuestion;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiQuestionRequest;

public interface AiQuestionGenerator {
    AiGeneratedQuestion generate(AiQuestionRequest request);
    boolean isAvailable();
}