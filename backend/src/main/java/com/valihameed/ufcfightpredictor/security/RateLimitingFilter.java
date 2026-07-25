package com.valihameed.ufcfightpredictor.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Cache<String, Bucket> cache = Caffeine.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).maximumSize(10000).build();

    @Value("${rate-limiting.global.capacity:100}")
    private long globalCapacity;
    @Value("${rate-limiting.global.refill:100}")
    private long globalRefill;
    @Value("${rate-limiting.global.refill-period-seconds:60}")
    private long globalRefillPeriod;

    @Value("${rate-limiting.login.capacity:5}")
    private long loginCapacity;
    @Value("${rate-limiting.login.refill:5}")
    private long loginRefill;
    @Value("${rate-limiting.login.refill-period-seconds:60}")
    private long loginRefillPeriod;

    @Value("${rate-limiting.prediction-submit.capacity:5}")
    private long predictionSubmitCapacity;
    @Value("${rate-limiting.prediction-submit.refill:5}")
    private long predictionSubmitRefill;
    @Value("${rate-limiting.prediction-submit.refill-period-seconds:60}")
    private long predictionSubmitRefillPeriod;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();
        String path = request.getRequestURI();

        // GET/HEAD/OPTIONS requests are read-only — never rate-limit them.
        // This prevents prediction spam from starving the global bucket and
        // blocking server-side rendering requests that fetch events/fights.
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = extractClientIp(request);

        // Global write-request bucket
        String key = ip + ":global_write";
        Bucket bucket = cache.get(key, k -> newBucket(globalCapacity, globalRefill, globalRefillPeriod));
        if (!bucket.tryConsume(1)) {
            sendRateLimit(response);
            return;
        }

        // Path-specific tighter buckets for mutating endpoints
        if (path.startsWith("/api/v1/internal/scraper")) {
            // Scraper gets its own bucket so it doesn't starve the global bucket
            Bucket b = cache.get(ip + ":scraper", k -> newBucket(100, 100, 60));
            if (!b.tryConsume(1)) {
                sendRateLimit(response);
                return;
            }
            // Scraper bypasses the global user bucket to avoid affecting users
            filterChain.doFilter(request, response);
            return;
        } else if (path.startsWith("/api/v1/auth/login")) {
            Bucket b = cache.get(ip + ":login", k -> newBucket(loginCapacity, loginRefill, loginRefillPeriod));
            if (!b.tryConsume(1)) {
                sendRateLimit(response);
                return;
            }
        } else if (path.startsWith("/api/v1/predictions") && "POST".equalsIgnoreCase(method)) {
            Bucket b = cache.get(ip + ":prediction_submit", k -> newBucket(predictionSubmitCapacity, predictionSubmitRefill, predictionSubmitRefillPeriod));
            if (!b.tryConsume(1)) {
                sendRateLimit(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Bucket newBucket(long capacity, long refill, long periodSeconds) {
        Refill refillStrategy = Refill.intervally(refill, Duration.ofSeconds(periodSeconds));
        Bandwidth limit = Bandwidth.classic(capacity, refillStrategy);
        return Bucket4j.builder().addLimit(limit).build();
    }

    private void sendRateLimit(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", "1");
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"RATE_LIMITED\",\"message\":\"Too Many Requests\"}");
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
