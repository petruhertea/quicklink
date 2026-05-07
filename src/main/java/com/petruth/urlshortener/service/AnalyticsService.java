package com.petruth.urlshortener.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.petruth.urlshortener.dto.ClickContext;
import com.petruth.urlshortener.entity.ClickAnalytics;
import com.petruth.urlshortener.entity.ShortenedUrl;
import com.petruth.urlshortener.repository.ClickAnalyticsRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final ClickAnalyticsRepository analyticsRepository;
    private final Logger log = LoggerFactory.getLogger(AnalyticsService.class);
    private static final String GEO_API_URL = "https://ipwho.is/%s";
    private final RestTemplate restTemplate;

    // IP geo results don't change — cache them for 24 hours
    private final Cache<String, Map<String, String>> geoCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build();

    public AnalyticsService(ClickAnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;

        // 2 second connect + read timeout — geo lookup is best-effort
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Async
    public void recordClick(ShortenedUrl url, ClickContext ctx) {
        ClickAnalytics analytics = new ClickAnalytics();
        analytics.setShortenedUrl(url);
        analytics.setIpAddress(ctx.ipAddress());
        analytics.setUserAgent(ctx.userAgent());
        analytics.setReferer(extractDomain(ctx.referer()));

        if (ctx.userAgent() != null) {
            analytics.setDeviceType(detectDeviceType(ctx.userAgent()));
            analytics.setBrowser(detectBrowser(ctx.userAgent()));
            analytics.setOs(detectOS(ctx.userAgent()));
        }

        Map<String, String> geo = getGeolocation(ctx.ipAddress());
        analytics.setCountry(geo.get("country"));
        analytics.setCity(geo.get("city"));

        analyticsRepository.save(analytics);
    }

    private Map<String, String> getGeolocation(String ipAddress) {
        Map<String, String> empty = Map.of("country", null, "city", null);

        if (ipAddress == null
                || ipAddress.equals("127.0.0.1")
                || ipAddress.startsWith("192.168.")
                || ipAddress.startsWith("10.")
                || ipAddress.equals("0:0:0:0:0:0:0:1")) {
            return empty;
        }

        // Return cached result if we've seen this IP before
        return geoCache.get(ipAddress, ip -> {
            try {
                String url = GEO_API_URL.formatted(ip);
                Map<String, Object> response = restTemplate.getForObject(url, Map.class);
                if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                    return Map.of(
                            "country", (String) response.getOrDefault("country", null),
                            "city",    (String) response.getOrDefault("city", null)
                    );
                }
            } catch (Exception e) {
                log.warn("Geo lookup failed for {}: {}", ip, e.getMessage());
            }
            return empty;
        });
    }

    private String extractDomain(String referer) {
        if (referer == null || referer.trim().isEmpty()) {
            return "Direct";
        }
        try {
            URL url = URI.create(referer).toURL();
            String host = url.getHost();
            // Strip www. prefix
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception _) {
            return "Direct";
        }
    }

    public Map<String, Object> getAnalyticsForUrl(ShortenedUrl url, int days) {
        Map<String, Object> analytics = new HashMap<>();

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        // Daily clicks
        List<Object[]> dailyClicks = analyticsRepository.getClicksByDay(url, startDate);
        analytics.put("dailyClicks", formatDailyClicks(dailyClicks));

        // Country breakdown
        List<Object[]> countryClicks = analyticsRepository.getClicksByCountry(url);
        analytics.put("countries", formatBreakdown(countryClicks));

        // Device breakdown
        List<Object[]> deviceClicks = analyticsRepository.getClicksByDevice(url);
        analytics.put("devices", formatBreakdown(deviceClicks));

        // Browser breakdown
        List<Object[]> browserClicks = analyticsRepository.getClicksByBrowser(url);
        analytics.put("browsers", formatBreakdown(browserClicks));

        // Top referrers
        List<Object[]> refererClicks = analyticsRepository.getClicksByReferer(url);
        analytics.put("referrers", formatBreakdown(refererClicks));

        return analytics;
    }

    private List<Map<String, Object>> formatDailyClicks(List<Object[]> data) {
        return data.stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("date", row[0].toString());
                    map.put("count", row[1]);
                    return map;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> formatBreakdown(List<Object[]> data) {
        return data.stream()
                .limit(10) // Top 10
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("label", row[0] != null ? row[0].toString() : "Unknown");
                    map.put("count", row[1]);
                    return map;
                })
                .collect(Collectors.toList());
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String detectDeviceType(String userAgent) {
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return "Mobile";
        } else if (ua.contains("tablet") || ua.contains("ipad")) {
            return "Tablet";
        }
        return "Desktop";
    }

    private String detectBrowser(String userAgent) {
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg")) return "Edge";
        if (ua.contains("chrome")) return "Chrome";
        if (ua.contains("firefox")) return "Firefox";
        if (ua.contains("safari") && !ua.contains("chrome")) return "Safari";
        if (ua.contains("opera") || ua.contains("opr")) return "Opera";
        return "Other";
    }

    private String detectOS(String userAgent) {
        String ua = userAgent.toLowerCase();
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("mac")) return "MacOS";
        if (ua.contains("linux")) return "Linux";
        if (ua.contains("android")) return "Android";
        if (ua.contains("iphone") || ua.contains("ipad")) return "iOS";
        return "Other";
    }
}