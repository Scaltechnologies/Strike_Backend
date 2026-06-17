package com.card_service.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secret;

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
                    Key key = Keys.hmacShaKeyFor(secret.getBytes());
                    Claims claims = Jwts.parserBuilder()
                            .setSigningKey(key).build()
                            .parseClaimsJws(authHeader.substring(7)).getBody();
                    userId = claims.getSubject();
                    role   = claims.get("role", String.class);
                } catch (Exception ignored) {}
            }
        }

        if (userId != null && !userId.isBlank() && role != null && !role.isBlank()) {
            try {
                var auth = new UsernamePasswordAuthenticationToken(
                        Long.parseLong(userId),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (NumberFormatException ignored) {}
        }

        chain.doFilter(request, response);
    }
}