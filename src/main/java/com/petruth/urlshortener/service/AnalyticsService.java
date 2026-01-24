package com.petruth.urlshortener.service;

import com.petruth.urlshortener.entity.ClickAnalytics;
import com.petruth.urlshortener.entity.ShortenedUrl;
import com.petruth.urlshortener.repository.ClickAnalyticsRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
        analytics.setIpAddress(getClientIP(request));
        analytics.setUserAgent(request.getHeader("User-Agent"));
        analytics.setReferer(request.getHeader("Referer"));

        // Parse user agent for device, browser, OS
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            analytics.setDeviceType(detectDeviceType(userAgent));
            analytics.setBrowser(detectBrowser(userAgent));
            analytics.setOs(detectOS(userAgent));
        }

        // Note: For geolocation, you'd need a service like MaxMind GeoIP2
        // For now, we'll leave country/city as null or use a simple implementation

        analyticsRepository.save(analytics);
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