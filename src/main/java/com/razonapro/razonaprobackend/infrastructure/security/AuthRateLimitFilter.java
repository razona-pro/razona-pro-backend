package com.razonapro.razonaprobackend.infrastructure.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final int      MAX_REQUESTS   = 10;
    private static final long     WINDOW_MINUTES = 1;
    // Endpoints sensibles que re-validan credenciales (login, recuperación, apelaciones públicas).
    private static final String[] ENDPOINT_PREFIXES = {
            "/api/auth/", "/api/appeals/account-status", "/api/appeals/submit" };

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(
                MAX_REQUESTS,
                Refill.intervally(MAX_REQUESTS, Duration.ofMinutes(WINDOW_MINUTES))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        String uri = req.getRequestURI();
        boolean rateLimited = false;
        for (String prefix : ENDPOINT_PREFIXES) {
            if (uri.startsWith(prefix)) { rateLimited = true; break; }
        }
        if (!rateLimited) {
            chain.doFilter(req, res);
            return;
        }

        String clientIp = req.getRemoteAddr();
        Bucket bucket   = buckets.computeIfAbsent(clientIp, k -> newBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(req, res);
        } else {
            log.warn("Rate limit excedido para IP {} en {}", clientIp, req.getRequestURI());
            res.setStatus(429);
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.getWriter().write(
                    "{\"success\":false,\"code\":\"AUTH-008\","
                            + "\"message\":\"Demasiados intentos. Espera un momento e intenta de nuevo.\"}");
        }
    }
}