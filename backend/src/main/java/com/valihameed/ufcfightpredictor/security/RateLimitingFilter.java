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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String ip = extractClientIp(request);
        String key = ip + ":global";
        Bucket bucket = cache.get(key, k -> newBucket(globalCapacity, globalRefill, globalRefillPeriod));
        if (bucket.tryConsume(1)) {
            // path-specific tighter buckets
            String path = request.getRequestURI();
            if (path.startsWith("/api/v1/auth/login")) {
                Bucket b = cache.get(ip + ":login", k -> newBucket(loginCapacity, loginRefill, loginRefillPeriod));
                if (!b.tryConsume(1)) {
                    sendRateLimit(response, b);
                    return;
                }
            }
            filterChain.doFilter(request, response);
        } else {
            sendRateLimit(response, bucket);
        }
    }

    private Bucket newBucket(long capacity, long refill, long periodSeconds) {
        Refill refillStrategy = Refill.intervally(refill, Duration.ofSeconds(periodSeconds));
        Bandwidth limit = Bandwidth.classic(capacity, refillStrategy);
        return Bucket4j.builder().addLimit(limit).build();
    }

    private void sendRateLimit(HttpServletResponse response, Bucket bucket) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", "1");
        response.getWriter().write("Too Many Requests");
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
