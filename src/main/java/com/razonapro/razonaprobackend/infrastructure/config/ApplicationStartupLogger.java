package com.razonapro.razonaprobackend.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationStartupLogger {

    private final Environment   env;
    private final AppProperties appProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void logStartup() {
        String frontend = appProperties.getFrontendUrl();
        String profile  = String.join(",", env.getActiveProfiles());
        if (profile.isBlank()) profile = "default";

        String schema     = env.getProperty("spring.jpa.properties.hibernate.default_schema", "razonapro");
        String aiProvider = env.getProperty("ai.model.provider", "NONE");
        boolean aiOn      = Boolean.parseBoolean(env.getProperty("ai.model.enabled", "false"));

        log.info("");
        log.info("════════════════════════════════════════════════════════════");
        log.info("  RAZONAPRO BACKEND — listo");
        log.info("  Profile  : {}", profile);
        log.info("  Frontend : {}", frontend);
        log.info("  Schema   : {}", schema);
        log.info("  AI       : {} ({})", aiOn ? "ON" : "OFF", aiProvider);
        log.info("════════════════════════════════════════════════════════════");
        log.info("");
    }
}