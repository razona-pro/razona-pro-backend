// src/main/java/com/razonapro/razonaprobackend/domain/aitried/service/AiSessionStore.java
package com.razonapro.razonaprobackend.domain.aitried.service;

import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiGeneratedQuestion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estado efímero de la sesión adaptativa.
 * Nota: en-memoria → no sobrevive reinicios del servidor.
 * Para producción multi-instancia, sustituir por Redis.
 */
@Slf4j
@Component
public class AiSessionStore {

    private static final long SESSION_TTL_SECONDS = 7200L; // 2 horas

    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    // ── Estado de sesión ─────────────────────────────────────────────

    public static class SessionState {
        public double                theta      = 0.0;
        public int                   servedCount = 0;
        public boolean               awaitingAnswer = false;
        public AiGeneratedQuestion   currentQuestion;
        public List<String>          usedStatements = new ArrayList<>();
        public String                competenceId;
        public String                competenceName;
        public String                competenceDescription;
        public Instant               lastAccess = Instant.now();

        void touch() { lastAccess = Instant.now(); }
    }

    // ── API ────────────────────────────────────────────────────────────

    public void put(String aiTriedId, SessionState state) {
        sessions.put(aiTriedId, state);
        log.debug("Sesión AI creada: {}", aiTriedId);
    }

    public Optional<SessionState> get(String aiTriedId) {
        SessionState s = sessions.get(aiTriedId);
        if (s != null) s.touch();
        return Optional.ofNullable(s);
    }

    public void remove(String aiTriedId) {
        sessions.remove(aiTriedId);
        log.debug("Sesión AI eliminada: {}", aiTriedId);
    }

    // ── Limpieza periódica ─────────────────────────────────────────────

    @Scheduled(fixedDelay = 600_000) // cada 10 minutos
    public void evictExpired() {
        Instant cutoff = Instant.now().minusSeconds(SESSION_TTL_SECONDS);
        int before = sessions.size();
        sessions.entrySet().removeIf(e -> e.getValue().lastAccess.isBefore(cutoff));
        int evicted = before - sessions.size();
        if (evicted > 0) log.info("Sesiones AI expiradas eliminadas: {}", evicted);
    }
}