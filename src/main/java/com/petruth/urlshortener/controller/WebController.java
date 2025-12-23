package com.petruth.urlshortener.controller;

import com.petruth.urlshortener.entity.ShortenedUrl;
import com.petruth.urlshortener.entity.User;
import com.petruth.urlshortener.service.ShortenedUrlServiceImpl;
import com.petruth.urlshortener.service.UserServiceImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WebController {

    private final UserServiceImpl userService;
    private final ShortenedUrlServiceImpl shortenedUrlService;

    public WebController(UserServiceImpl userService, ShortenedUrlServiceImpl shortenedUrlService) {
        this.userService = userService;
        this.shortenedUrlService = shortenedUrlService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal OAuth2User principal, OAuth2AuthenticationToken authToken,Model model) {
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
}