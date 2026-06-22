package com.card_service.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.List;

/**
 * Authenticates requests from two sources:
 * 1. Gateway-injected X-User-Id / X-User-Role headers (normal production path).
 * 2. Bearer JWT in the Authorization header (Swagger UI / direct service calls).
 */
@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secret;

    private Key signingKey;

    @PostConstruct
    void initKey() {
        signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String role   = request.getHeader("X-User-Role");

        // Fallback: parse JWT directly when gateway headers are absent
        if (userId == null || userId.isBlank() || role == null || role.isBlank()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    Key key = signingKey;
                    Claims claims = Jwts.parserBuilder()
                            .setSigningKey(key).build()
                            .parseClaimsJws(authHeader.substring(7)).getBody();
                    userId = claims.getSubject();
                    role   = claims.get("role", String.class);
                    log.debug("[JwtAuthFilter] card-service — JWT parsed directly: userId={} role={}", userId, role);
                } catch (Exception ex) {
                    log.warn("[JwtAuthFilter] card-service — JWT parse failed ({}): {}",
                            ex.getClass().getSimpleName(), ex.getMessage());
                }
            }
        } else {
            log.debug("[JwtAuthFilter] card-service — headers present: X-User-Id={} X-User-Role={}", userId, role);
        }

        if (userId != null && !userId.isBlank() && role != null && !role.isBlank()) {
            try {
                var auth = new UsernamePasswordAuthenticationToken(
                        Long.parseLong(userId),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("[JwtAuthFilter] card-service — authentication set: principal={} role={}", userId, role);
            } catch (NumberFormatException ex) {
                log.warn("[JwtAuthFilter] card-service — X-User-Id '{}' is not a valid Long", userId);
            }
        } else {
            log.debug("[JwtAuthFilter] card-service — no credentials found, request proceeds unauthenticated");
        }

        chain.doFilter(request, response);
    }
}
