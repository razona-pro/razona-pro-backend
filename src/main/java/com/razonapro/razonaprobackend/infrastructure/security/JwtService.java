package com.razonapro.razonaprobackend.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAdminToken(String adminId, String email) {
        return Jwts.builder()
                .subject(adminId)
                .claim("email", email)
                .claim("userType", "ADMIN")
                .claim("role", "ROLE_ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key())
                .compact();
    }

    public String generateStudentToken(String studentId, String programId, String email) {
        return Jwts.builder()
                .subject(studentId)
                .claim("email", email)
                .claim("programId", programId)
                .claim("userType", "STUDENT")
                .claim("role", "ROLE_STUDENT")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    public String getSubject(String token)   { return parseToken(token).getSubject(); }
    public String getUserType(String token)  { return parseToken(token).get("userType", String.class); }
    public String getProgramId(String token) { return parseToken(token).get("programId", String.class); }
    public String getRole(String token)      { return parseToken(token).get("role", String.class); }
}