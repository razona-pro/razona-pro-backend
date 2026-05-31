// src/main/java/com/razonapro/razonaprobackend/infrastructure/config/AiModelProperties.java
package com.razonapro.razonaprobackend.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "ai.model")
public class AiModelProperties {

    private boolean enabled = false;
    private Provider provider = Provider.NONE;

    private String hfApiToken;
    private String hfEndpoint;
    private String hfModelId;

    private String localPath;
    private double minConfidence = 0.7;

    // Ollama (fallback local)
    private String ollamaBaseUrl = "http://localhost:11434";
    private String ollamaModel   = "mistral";
    private int    ollamaTimeout = 120;
    private double ollamaTemperature = 0.7;
    private int    ollamaMaxRetries  = 3;

    // Cloud (Groq/OpenAI/OpenRouter — protocolo OpenAI)
    private String cloudBaseUrl     = "https://api.groq.com/openai/v1";
    private String cloudApiKey;
    private String cloudModel       = "llama-3.3-70b-versatile";
    private int    cloudTimeout     = 60;
    private double cloudTemperature = 0.7;
    private int    cloudMaxRetries  = 3;

    public enum Provider { NONE, HUGGINGFACE, LOCAL, OLLAMA, CLOUD }
}