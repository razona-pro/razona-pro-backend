package com.razonapro.razonaprobackend.infrastructure.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupJob {

    private final JdbcTemplate jdbc;

    @Value("${spring.jpa.properties.hibernate.default_schema:razonapro}")
    private String schema;

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanup() {
        int admins   = jdbc.update(
                "DELETE FROM " + schema + ".admin_tokens   WHERE expires_at < NOW() - INTERVAL '7 days'");
        int students = jdbc.update(
                "DELETE FROM " + schema + ".student_tokens WHERE expires_at < NOW() - INTERVAL '7 days'");
        log.info("TokenCleanupJob - admin_tokens borrados: {}, student_tokens borrados: {}", admins, students);
    }
}