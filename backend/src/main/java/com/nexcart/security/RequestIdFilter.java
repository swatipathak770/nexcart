package com.nexcart.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

public class RequestIdFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String suppliedId = request.getHeader("X-Request-Id");
        String requestId = suppliedId != null && SAFE_ID.matcher(suppliedId).matches()
                ? suppliedId : UUID.randomUUID().toString();
        long started = System.nanoTime();
        MDC.put("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - started) / 1_000_000;
            int status = response.getStatus();
            String path = request.getServletPath();
            if (path == null || path.isBlank()) path = request.getRequestURI();
            if (status >= 500) {
                log.warn("HTTP {} {} completed status={} durationMs={}", request.getMethod(), path, status, elapsedMs);
            } else {
                log.info("HTTP {} {} completed status={} durationMs={}", request.getMethod(), path, status, elapsedMs);
            }
            MDC.remove("requestId");
        }
    }
}
