package org.example.backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Per-IP rate limit filter (Quick Win 3).
 *
 * <p>Buckets are split per endpoint class so brute-force on auth cannot be
 * drowned by ordinary traffic on the API:
 * <ul>
 *   <li>{@code POST /api/auth/login} — 10 requests / minute / IP</li>
 *   <li>{@code POST /api/auth/register} — 5 requests / hour / IP</li>
 *   <li>{@code /api/ai/**} — 30 requests / minute / IP</li>
 *   <li>everything else — 600 requests / minute / IP</li>
 * </ul>
 *
 * <p>Returns {@code 429 Too Many Requests} with a JSON body and standard
 * {@code X-RateLimit-*} headers when the bucket is empty.
 *
 * <p>Uses Bucket4j for token-bucket semantics. In-memory store is appropriate
 * for single-instance deployments; for horizontal scaling swap in
 * bucket4j-redis so all nodes share counters.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter implements Filter {

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> aiBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> defaultBuckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();
        String method = req.getMethod();
        String ip = clientIp(req);

        Bucket bucket = pickBucket(path, method, ip);
        if (bucket == null) {
            chain.doFilter(request, response);
            return;
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        res.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, probe.getRemainingTokens())));

        if (probe.isConsumed()) {
            chain.doFilter(request, response);
            return;
        }

        long waitSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
        res.setHeader("X-RateLimit-Retry-After-Seconds", String.valueOf(Math.max(1, waitSeconds)));
        res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.getWriter().write(
                "{\"error\":\"Quá nhiều yêu cầu. Vui lòng thử lại sau " + waitSeconds + " giây.\","
                        + "\"retryAfterSeconds\":" + waitSeconds + "}");
    }

    private Bucket pickBucket(String path, String method, String ip) {
        // Excluded paths
        if (path.startsWith("/uploads/") || path.startsWith("/ws") || path.startsWith("/topic")
                || path.startsWith("/actuator")) {
            return null;
        }
        if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/auth/login")) {
            return loginBuckets.computeIfAbsent(ip, k -> newBucket(10, Duration.ofMinutes(1)));
        }
        if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/auth/register")) {
            return registerBuckets.computeIfAbsent(ip, k -> newBucket(5, Duration.ofHours(1)));
        }
        if (path.startsWith("/api/ai/")) {
            return aiBuckets.computeIfAbsent(ip, k -> newBucket(30, Duration.ofMinutes(1)));
        }
        // Keep legacy exclusion for /api/auth/login from the default counter so
        // logged-in traffic doesn't drain brute-force budget and vice versa.
        if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register")
                || path.startsWith("/api/debug")) {
            return null;
        }
        return defaultBuckets.computeIfAbsent(ip, k -> newBucket(600, Duration.ofMinutes(1)));
    }

    private static Bucket newBucket(long capacity, Duration period) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(capacity, Refill.intervally(capacity, period)))
                .build();
    }

    private static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String real = req.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
        return req.getRemoteAddr();
    }
}