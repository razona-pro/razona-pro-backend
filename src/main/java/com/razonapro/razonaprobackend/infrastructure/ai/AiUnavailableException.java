// src/main/java/com/razonapro/razonaprobackend/infrastructure/ai/AiUnavailableException.java
package com.razonapro.razonaprobackend.infrastructure.ai;

public class AiUnavailableException extends RuntimeException {
    public AiUnavailableException(String msg) { super(msg); }
}