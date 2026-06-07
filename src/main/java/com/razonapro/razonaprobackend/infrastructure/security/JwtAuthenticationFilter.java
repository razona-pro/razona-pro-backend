package com.razonapro.razonaprobackend.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.razonapro.razonaprobackend.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final StudentRepository studentRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtService.isValid(token)) {
            try {
                String userId    = jwtService.getSubject(token);
                String role      = jwtService.getRole(token);
                String userType  = jwtService.getUserType(token);
                String programId = jwtService.getProgramId(token);

                // Si el estudiante fue desactivado, su token deja de ser válido de inmediato.
                // En rutas protegidas devolvemos 401 ACCOUNT_DISABLED explícito para que el
                // front lo saque de inmediato con una alerta. En rutas públicas (login,
                // apelaciones) seguimos como anónimo para que pueda apelar.
                if ("STUDENT".equals(userType) && !studentRepository.existsByStudentIdAndIsActiveTrue(userId)) {
                    String uri = request.getRequestURI();
                    boolean publicPath = uri.startsWith("/api/auth") || uri.startsWith("/api/appeals")
                            || uri.startsWith("/api/health") || uri.startsWith("/api/programs/active");
                    if (publicPath) {
                        chain.doFilter(request, response);
                    } else {
                        response.setStatus(401);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write(
                                "{\"success\":false,\"code\":\"ACCOUNT_DISABLED\","
                                + "\"message\":\"Tu cuenta fue desactivada. No puedes seguir navegando.\"}");
                    }
                    return;
                }

                UserPrincipal principal = UserPrincipal.builder()
                        .id(userId)
                        .programId(programId)
                        .userType(userType)
                        .authorities(List.of(new SimpleGrantedAuthority(role)))
                        .build();

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                log.error("Error estableciendo autenticación: {}", e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}