package com.nexcart.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Simple per-instance fixed-window limits for sensitive write endpoints. */
public class ApiRateLimitFilter extends OncePerRequestFilter {
    private static final long WINDOW_MS = 60_000;
    private static final int MAX_KEYS = 20_000;
    private static final Pattern IP_CHARS = Pattern.compile("[0-9A-Fa-f:.]{1,64}");
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanup = new AtomicLong();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Limit limit = limitFor(request);
        if (limit == null) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = System.currentTimeMillis();
        String key = limit.name + ':' + clientAddress(request);
        AtomicBoolean rejected = new AtomicBoolean();
        windows.compute(key, (ignored, current) -> {
            Window next = current == null || now - current.start >= WINDOW_MS
                    ? new Window(now, 1)
                    : new Window(current.start, current.count + 1);
            rejected.set(next.count > limit.maximum);
            return next;
        });
        cleanupOccasionally(now);

        if (rejected.get()) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private Limit limitFor(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return null;
        String path = request.getServletPath();
        if (path == null || path.isBlank()) path = request.getRequestURI();
        if (path.equals("/api/auth/login")) return new Limit("login", 10);
        if (path.equals("/api/auth/register")) return new Limit("register", 5);
        if (path.startsWith("/api/payments/")) return new Limit("payments", 40);
        if (path.startsWith("/api/admin/recovery/") || path.startsWith("/api/recovery/")) {
            return new Limit("recovery", 30);
        }
        // Webhooks intentionally have their own signature verification and retry contract.
        return null;
    }

    private String clientAddress(HttpServletRequest request) {
        // Nginx overwrites this header with its connected client's address.
        String forwardedAddress = request.getHeader("X-Real-IP");
        if (forwardedAddress != null && IP_CHARS.matcher(forwardedAddress.trim()).matches()) {
            return forwardedAddress.trim();
        }
        return request.getRemoteAddr();
    }

    private void cleanupOccasionally(long now) {
        if (windows.size() <= MAX_KEYS || now - lastCleanup.get() < WINDOW_MS) return;
        if (!lastCleanup.compareAndSet(lastCleanup.get(), now)) return;
        windows.entrySet().removeIf(entry -> now - entry.getValue().start > 2 * WINDOW_MS);
    }

    private record Limit(String name, int maximum) { }
    private record Window(long start, int count) { }
}
