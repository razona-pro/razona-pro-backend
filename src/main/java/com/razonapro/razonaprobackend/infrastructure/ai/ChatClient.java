// src/main/java/com/razonapro/razonaprobackend/infrastructure/ai/ChatClient.java
package com.razonapro.razonaprobackend.infrastructure.ai;

public interface ChatClient {
    String chat(String systemPrompt, String userMessage, boolean jsonFormat);
    boolean ping();
}