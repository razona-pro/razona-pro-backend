package com.razonapro.razonaprobackend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    @Value("${jwt.email-verification-expiration-ms}")
    private long emailVerifyExpirationMs;

    private SecretKey key() {
        byte[] bytes = Decoders.BASE64.decode(java.util.Base64.getEncoder().encodeToString(secret.getBytes()));
        return Keys.hmacShaKeyFor(bytes);
    }

    /** Token de acceso para Admin */
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

    /** Token de acceso para Student */
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

    /** Token para verificación de email (firmado, sin rol) */
    public String generateEmailVerificationToken(String studentId, String programId, String email) {
        return Jwts.builder()
            .subject(studentId)
            .claims(Map.of("programId", programId, "email", email, "purpose", "EMAIL_VERIFY"))
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + emailVerifyExpirationMs))
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
