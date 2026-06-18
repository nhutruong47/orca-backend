package org.example.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter implements jakarta.servlet.Filter {

    private static final int MAX_REQUESTS_PER_MINUTE = 600;
    private static final int WINDOW_SECONDS = 60;

    private static class ClientBucket {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long windowStart = Instant.now().getEpochSecond();
    }

    private final Map<String, ClientBucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String clientId = getClientId(req);
        String path = req.getRequestURI();

        if (isExcluded(path)) {
            chain.doFilter(request, response);
            return;
        }

        ClientBucket bucket = buckets.computeIfAbsent(clientId, k -> new ClientBucket());
        long now = Instant.now().getEpochSecond();
        long elapsed = now - bucket.windowStart;

        if (elapsed >= WINDOW_SECONDS) {
            bucket.count.set(0);
            bucket.windowStart = now;
            elapsed = 0;
        }

        int current = bucket.count.incrementAndGet();
        if (current > MAX_REQUESTS_PER_MINUTE) {
            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
            return;
        }

        res.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
        res.setHeader("X-RateLimit-Remaining", String.valueOf(MAX_REQUESTS_PER_MINUTE - current));
        res.setHeader("X-RateLimit-Reset", String.valueOf(bucket.windowStart + WINDOW_SECONDS));

        chain.doFilter(request, response);
    }

    private String getClientId(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty()) {
            return xf.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private boolean isExcluded(String path) {
        return path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/register")
                || path.startsWith("/api/debug")
                || path.startsWith("/actuator")
                || path.startsWith("/ws");
    }
}
