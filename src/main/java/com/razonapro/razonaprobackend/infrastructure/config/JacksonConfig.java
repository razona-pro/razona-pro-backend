// src/main/java/com/razonapro/razonaprobackend/infrastructure/config/JacksonConfig.java
package com.razonapro.razonaprobackend.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}