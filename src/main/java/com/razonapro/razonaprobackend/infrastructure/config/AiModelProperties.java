// src/main/java/com/razonapro/razonaprobackend/infrastructure/config/AiModelProperties.java
package com.razonapro.razonaprobackend.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai.model")
public class AiModelProperties {

    private boolean enabled = false;
    private Provider provider = Provider.NONE;

    // HuggingFace
    private String hfApiToken;
    private String hfEndpoint;
    private String hfModelId;

    // Local
    private String localPath;
    private double minConfidence = 0.7;

    // Ollama
    private String ollamaBaseUrl = "http://localhost:11434";
    private String ollamaModel   = "mistral";
    private int    ollamaTimeout = 120;   // segundos
    private double ollamaTemperature = 0.7;
    private int    ollamaMaxRetries  = 3;

    public enum Provider {
        NONE,
        HUGGINGFACE,
        LOCAL,
        OLLAMA
    }
}