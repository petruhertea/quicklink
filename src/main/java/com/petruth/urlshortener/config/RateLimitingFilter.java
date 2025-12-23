package com.petruth.urlshortener.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter implements Filter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Only rate limit the shorten endpoint
        if (httpRequest.getRequestURI().equals("/api/shorten") &&
                httpRequest.getMethod().equals("POST")) {

            String key = getClientKey(httpRequest);
            Bucket bucket = resolveBucket(key);

            if (bucket.tryConsume(1)) {
                chain.doFilter(request, response);
            } else {
                httpResponse.setStatus(429);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"error\":\"Too many requests. Please try again in a minute.\"}");
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private Bucket resolveBucket(String key) {
        return cache.computeIfAbsent(key, k -> createNewBucket(k));
    }

    private Bucket createNewBucket(String key) {
        // Premium users get 500 requests per minute
        // Authenticated users get 50 requests per minute
        // Anonymous users get 10 requests per minute
        int capacity;

        if (key.startsWith("premium:")) {
            capacity = 500;
        } else if (key.startsWith("auth:")) {
            capacity = 50;
        } else {
            capacity = 10;
        }

        Bandwidth limit = Bandwidth.classic(capacity,
                Refill.intervally(capacity, Duration.ofMinutes(1)));

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private String getClientKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // If user is authenticated, check if premium
        if (auth != null && auth.isAuthenticated() &&
                !auth.getName().equals("anonymousUser")) {

            // You'll need to check if user is premium here
            // For now, we'll use auth: prefix
            // You can enhance this later to check user.isPremium()
            return "auth:" + auth.getName();
        }

        // Otherwise use IP address
        String ip = getClientIP(request);
        return "ip:" + ip;
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}