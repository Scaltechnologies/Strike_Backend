package com.user_service.config;

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
 * Reads X-User-Id and X-User-Role headers (injected by the JwtHeaderFilter from the JWT)
 * and sets a Spring Security Authentication in the SecurityContext.
 * Falls back to Bearer JWT parsing for direct / Swagger access.
 * This enables @PreAuthorize and URL-based role checks to work.
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

        String userId = null;
        String role   = null;

        // Priority 1: Bearer JWT — cryptographically verified; the gateway always forwards it.
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(signingKey).build()
                        .parseClaimsJws(authHeader.substring(7)).getBody();
                userId = claims.getSubject();
                role   = claims.get("role", String.class);
                log.debug("[JwtAuthFilter] user-service — JWT verified: userId={} role={}", userId, role);
            } catch (Exception ex) {
                log.warn("[JwtAuthFilter] user-service — JWT parse failed ({}): {}",
                        ex.getClass().getSimpleName(), ex.getMessage());
            }
        }

        // Priority 2: Gateway-injected headers — fallback for internal calls without a Bearer token.
        if (userId == null || role == null) {
            String hUserId = request.getHeader("X-User-Id");
            String hRole   = request.getHeader("X-User-Role");
            if (hUserId != null && !hUserId.isBlank() && hRole != null && !hRole.isBlank()) {
                userId = hUserId;
                role   = hRole;
                log.debug("[JwtAuthFilter] user-service — using gateway headers: userId={} role={}", userId, role);
            }
        }

        if (userId != null && !userId.isBlank() && role != null && !role.isBlank()) {
            try {
                var auth = new UsernamePasswordAuthenticationToken(
                        Long.parseLong(userId),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("[JwtAuthFilter] user-service — authentication set: principal={} role={}", userId, role);
            } catch (NumberFormatException ex) {
                log.warn("[JwtAuthFilter] user-service — X-User-Id '{}' is not a valid Long", userId);
            }
        } else {
            log.debug("[JwtAuthFilter] user-service — no credentials found, request proceeds unauthenticated");
        }

        chain.doFilter(request, response);
    }
}
