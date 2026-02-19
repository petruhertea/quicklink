package com.petruth.urlshortener.service;

import com.petruth.urlshortener.entity.ClickAnalytics;
import com.petruth.urlshortener.entity.ShortenedUrl;
import com.petruth.urlshortener.repository.ClickAnalyticsRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final ClickAnalyticsRepository analyticsRepository;

    public AnalyticsService(ClickAnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    @Async
    public void recordClick(ShortenedUrl url, HttpServletRequest request) {
        ClickAnalytics analytics = new ClickAnalytics();
        analytics.setShortenedUrl(url);

        String ipAddress = getClientIP(request);
        analytics.setIpAddress(ipAddress);
        analytics.setUserAgent(request.getHeader("User-Agent"));
        analytics.setReferer(extractDomain(request.getHeader("Referer")));

        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            analytics.setDeviceType(detectDeviceType(userAgent));
            analytics.setBrowser(detectBrowser(userAgent));
            analytics.setOs(detectOS(userAgent));
        }

        // Geolocation
        Map<String, String> geo = getGeolocation(ipAddress);
        analytics.setCountry(geo.get("country"));
        analytics.setCity(geo.get("city"));

        analyticsRepository.save(analytics);
    }

    private static final String GEO_API_URL = "https://ipwho.is/%s";
    private final RestTemplate restTemplate = new RestTemplate();

    private Map<String, String> getGeolocation(String ipAddress) {
        Map<String, String> result = new HashMap<>();
        result.put("country", null);
        result.put("city", null);

        if (ipAddress == null ||
                ipAddress.equals("127.0.0.1") ||
                ipAddress.startsWith("192.168.") ||
                ipAddress.startsWith("10.") ||
                ipAddress.equals("0:0:0:0:0:0:0:1")) {
            System.out.println("GEO: Skipped private/local IP: " + ipAddress);
            return result;
        }

        try {
            String url = String.format(GEO_API_URL, ipAddress);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            System.out.println("GEO: Response: " + response);

            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                result.put("country", (String) response.get("country"));
                result.put("city", (String) response.get("city"));
            } else {
                System.out.println("GEO: Failed - success flag was false or response was null");
            }
        } catch (Exception e) {
            System.out.println("GEO: Exception - " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        return result;
    }

    private String extractDomain(String referer) {
        if (referer == null || referer.trim().isEmpty()) {
            return "Direct";
        }
        try {
            java.net.URL url = new java.net.URL(referer);
            String host = url.getHost();
            // Strip www. prefix
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception e) {
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