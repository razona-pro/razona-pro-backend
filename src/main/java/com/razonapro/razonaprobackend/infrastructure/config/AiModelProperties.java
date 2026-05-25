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

    private String hfApiToken;
    private String hfEndpoint;
    private String hfModelId;

    private String localPath;
    private double minConfidence = 0.7;

    public enum Provider {
        NONE,
        HUGGINGFACE,
        LOCAL
    }
}