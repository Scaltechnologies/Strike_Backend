package com.vendor_service.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.Key;
import java.util.*;

@Component
@Order(-200)
public class JwtHeaderFilter implements Filter {

    @Value("${jwt.secret}")
    private String secret;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // If gateway already injected X-User-Id, pass through as-is
        if (httpRequest.getHeader("X-User-Id") != null) {
            chain.doFilter(request, response);
            return;
        }

        // Try to extract from Authorization header directly
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                Key key = Keys.hmacShaKeyFor(secret.getBytes());
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key).build()
                        .parseClaimsJws(token).getBody();

                String userId = claims.getSubject();
                String role = (String) claims.get("role");
                String mobile = claims.get("mobile", String.class);

                chain.doFilter(new HeaderInjectedRequest(httpRequest, userId, role, mobile), response);
                return;
            } catch (Exception ignored) {}
        }

        chain.doFilter(request, response);
    }

    private static class HeaderInjectedRequest extends HttpServletRequestWrapper {
        private final Map<String, String> extraHeaders;

        HeaderInjectedRequest(HttpServletRequest request, String userId, String role, String mobile) {
            super(request);
            extraHeaders = new HashMap<>();
            if (userId != null) extraHeaders.put("X-User-Id", userId);
            if (role != null)   extraHeaders.put("X-User-Role", role);
            if (mobile != null) extraHeaders.put("X-User-Mobile", mobile);
        }

        @Override
        public String getHeader(String name) {
            String extra = extraHeaders.get(name);
            return extra != null ? extra : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String extra = extraHeaders.get(name);
            if (extra != null) return Collections.enumeration(List.of(extra));
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new HashSet<>(Collections.list(super.getHeaderNames()));
            names.addAll(extraHeaders.keySet());
            return Collections.enumeration(names);
        }
    }
}