package com.petruth.urlshortener.controller;

import com.petruth.urlshortener.entity.ShortenedUrl;
import com.petruth.urlshortener.entity.User;
import com.petruth.urlshortener.entity.UserOAuthProvider;
import com.petruth.urlshortener.repository.ClickAnalyticsRepository;
import com.petruth.urlshortener.service.ShortenedUrlServiceImpl;
import com.petruth.urlshortener.service.UserServiceImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WebController {

    private final UserServiceImpl userService;
    private final ShortenedUrlServiceImpl shortenedUrlService;
    private final ClickAnalyticsRepository clickAnalyticsRepository;

    public WebController(UserServiceImpl userService,
                         ShortenedUrlServiceImpl shortenedUrlService,
                         ClickAnalyticsRepository clickAnalyticsRepository) {
        this.userService = userService;
        this.shortenedUrlService = shortenedUrlService;
        this.clickAnalyticsRepository=clickAnalyticsRepository;
    }

    @GetMapping("/")
    public String home(@AuthenticationPrincipal OAuth2User principal,
                       OAuth2AuthenticationToken authToken,
                       Model model) {

        // If user is authenticated, add user data to model
        if (principal != null && authToken != null) {
            try {
                String provider = authToken.getAuthorizedClientRegistrationId();
                String oauthId;

                if ("google".equals(provider)) {
                    oauthId = principal.getAttribute("sub");
                } else if ("github".equals(provider)) {
                    Object idObj = principal.getAttribute("id");
                    oauthId = (idObj != null) ? idObj.toString() : null;
                } else {
                    oauthId = null;
                }

                if (oauthId != null) {
                    UserOAuthProvider oauthProvider = userService.findByOAuth(provider, oauthId)
                            .orElse(null);

                    if (oauthProvider != null) {
                        User user = oauthProvider.getUser();
                        model.addAttribute("user", user);
                    }
                }
            } catch (Exception e) {
                // If there's any error, just don't add the user to the model
                System.err.println("Error loading user for homepage: " + e.getMessage());
            }
        }

        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal OAuth2User principal, OAuth2AuthenticationToken authToken, Model model) {
        if (principal == null) {
            return "redirect:/";
        }

        String provider = authToken.getAuthorizedClientRegistrationId();
        String oauthId;

        if ("google".equals(provider)) {
            oauthId = principal.getAttribute("sub");
        } else if ("github".equals(provider)) {
            oauthId = principal.getAttribute("id").toString();
        } else {
            throw new RuntimeException("Unknown OAuth provider");
        }

        User user = userService.findByOAuth(provider, oauthId)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getUser();
        // Get user's URLs
        List<ShortenedUrl> urls = shortenedUrlService.findByUser(user);

        // Calculate stats
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalLinks", (long) urls.size());

        long totalClicks = urls.stream()
                .mapToLong(url -> url.getClickCount() != null ? url.getClickCount() : 0)
                .sum();
        stats.put("totalClicks", totalClicks);

        // Count clicks today
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long clicksToday = urls.stream()
                .filter(url -> url.getLastAccessed() != null &&
                        url.getLastAccessed().isAfter(startOfDay))
                .count();
        stats.put("clicksToday", clicksToday);

        // Sort URLs by creation date (newest first)
        urls.sort((a, b) -> b.getDateCreated().compareTo(a.getDateCreated()));

        model.addAttribute("user", user);
        model.addAttribute("urls", urls);
        model.addAttribute("stats", stats);

        return "dashboard";
    }

    @GetMapping("/analytics/{code}")
    public String analytics(@PathVariable String code,
                            @AuthenticationPrincipal OAuth2User principal,
                            OAuth2AuthenticationToken authToken,
                            Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        String provider = authToken.getAuthorizedClientRegistrationId();
        String oauthId;

        if ("google".equals(provider)) {
            oauthId = principal.getAttribute("sub");
        } else if ("github".equals(provider)) {
            Object idObj = principal.getAttribute("id");
            oauthId = (idObj != null) ? idObj.toString() : null;
        } else {
            throw new RuntimeException("Unknown OAuth provider");
        }

        User user = userService.findByOAuth(provider, oauthId)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getUser();

        ShortenedUrl url = shortenedUrlService.findByCode(code);

        if (url == null || !url.getUser().getId().equals(user.getId())) {
            return "redirect:/dashboard";
        }

        // Calculate stats
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfWeek = now.minusDays(7);

        Map<String, Long> stats = new HashMap<>();
        stats.put("clicksToday", clickAnalyticsRepository.countByShortenedUrlAndClickedAtAfter(url, startOfDay));
        stats.put("clicksThisWeek", clickAnalyticsRepository.countByShortenedUrlAndClickedAtAfter(url, startOfWeek));

        model.addAttribute("user", user);
        model.addAttribute("url", url);
        model.addAttribute("stats", stats);

        return "analytics";
    }

    @GetMapping("/subscription")
    public String subscription(@AuthenticationPrincipal OAuth2User principal,
                               OAuth2AuthenticationToken authToken,
                               Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        String provider = authToken.getAuthorizedClientRegistrationId();
        String oauthId;

        if ("google".equals(provider)) {
            oauthId = principal.getAttribute("sub");
        } else if ("github".equals(provider)) {
            oauthId = principal.getAttribute("id").toString();
        } else {
            throw new RuntimeException("Unknown OAuth provider");
        }

        User user = userService.findByOAuth(provider, oauthId)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getUser();

        List<ShortenedUrl> urls = shortenedUrlService.findByUser(user);

        // Calculate stats
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalLinks", (long) urls.size());

        long totalClicks = urls.stream()
                .mapToLong(url -> url.getClickCount() != null ? url.getClickCount() : 0)
                .sum();
        stats.put("totalClicks", totalClicks);

        model.addAttribute("user", user);
        model.addAttribute("stats", stats);

        return "subscription";
    }
}