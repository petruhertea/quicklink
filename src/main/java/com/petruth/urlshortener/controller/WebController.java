package com.petruth.urlshortener.controller;

import com.petruth.urlshortener.entity.ShortenedUrl;
import com.petruth.urlshortener.entity.User;
import com.petruth.urlshortener.entity.UserOAuthProvider;
import com.petruth.urlshortener.repository.ClickAnalyticsRepository;
import com.petruth.urlshortener.service.ShortenedUrlServiceImpl;
import com.petruth.urlshortener.service.UserServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

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
    private static final Logger log = LoggerFactory.getLogger(WebController.class);

    public WebController(UserServiceImpl userService,
                         ShortenedUrlServiceImpl shortenedUrlService,
                         ClickAnalyticsRepository clickAnalyticsRepository) {
        this.userService = userService;
        this.shortenedUrlService = shortenedUrlService;
        this.clickAnalyticsRepository = clickAnalyticsRepository;
    }

    @GetMapping("/")
    public String home(@AuthenticationPrincipal OAuth2User principal,
                       OAuth2AuthenticationToken authToken,
                       Model model) {
        if (principal != null && authToken != null) {
            try {
                User user = resolveUser(principal, authToken);
                model.addAttribute("user", user);
            } catch (Exception e) {
                System.err.println("Error loading user for homepage: " + e.getMessage());
            }
        }

        return "index";
    }

    // Replace the resolveUser method with this:
    private User resolveUser(OAuth2User principal, OAuth2AuthenticationToken authToken) {
        String provider = authToken.getAuthorizedClientRegistrationId();
        log.info("=== Resolving user from WebController ===");
        log.info("Provider: {}", provider);

        String oauthId;

        if ("github".equals(provider)) {
            Object idObj = principal.getAttribute("id");
            oauthId = idObj != null ? idObj.toString() : null;
            log.info("GitHub - Extracted ID: {}", oauthId);
        } else if ("google".equals(provider)) {
            oauthId = principal.getAttribute("sub");
            log.info("Google - Extracted sub: {}", oauthId);
        } else if ("microsoft".equals(provider)) {
            // Microsoft uses 'oid' or 'sub'
            oauthId = principal.getAttribute("oid");
            if (oauthId == null) {
                oauthId = principal.getAttribute("sub");
            }
            log.info("Microsoft - Extracted oid/sub: {}", oauthId);
        } else {
            Object idObj = principal.getAttribute("sub");
            if (idObj == null) {
                idObj = principal.getAttribute("id");
            }
            oauthId = idObj != null ? idObj.toString() : null;
            log.info("Unknown provider - Extracted ID: {}", oauthId);
        }

        if (oauthId == null) {
            log.error("OAuth ID is null for provider: {}", provider);
            log.error("Available attributes: {}", principal.getAttributes().keySet());
            throw new RuntimeException("OAuth ID missing for provider " + provider);
        }

        log.info("Querying database: provider={}, oauthId={}", provider, oauthId);

        String finalOauthId = oauthId;
        return userService.findByOAuth(provider, oauthId)
                .map(oauthProvider -> {
                    User user = oauthProvider.getUser();
                    log.info("Found user: ID={}, Email={}", user.getId(), user.getEmail());
                    return user;
                })
                .orElseThrow(() -> {
                    log.error("User not found for provider={}, oauthId={}", provider, finalOauthId);

                    // Debug: Let's see what we have in the database
                    try {
                        var allProviders = userService.findByEmail(principal.getAttribute("email"));
                        if (allProviders.isPresent()) {
                            User existingUser = allProviders.get();
                            log.error("BUT user exists with this email! User ID: {}, Email: {}",
                                    existingUser.getId(), existingUser.getEmail());
                            log.error("OAuth providers for this user: {}",
                                    existingUser.getOauthProviders().size());
                            existingUser.getOauthProviders().forEach(p ->
                                    log.error("  - Provider: {}, OAuth ID: {}", p.getOauthProvider(), p.getOauthId())
                            );
                        } else {
                            log.error("No user found with email: {}", (Object) principal.getAttribute("email"));
                        }
                    } catch (Exception e) {
                        log.error("Error during debug lookup", e);
                    }

                    return new RuntimeException("User not found");
                });
    }


    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/bulk-shorten")
    public String bulkShorten(@AuthenticationPrincipal OAuth2User principal,
                              OAuth2AuthenticationToken authToken,
                              Model model) {
        if (principal == null) {
            return "redirect:/";
        }

        if (principal != null && authToken != null) {
            try {
                User user = resolveUser(principal, authToken);
                model.addAttribute("user", user);
            } catch (Exception e) {
                System.err.println("Error loading user for homepage: " + e.getMessage());
            }
        }
        return "bulk-shorten";
    }

    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal OAuth2User principal,
            OAuth2AuthenticationToken authToken,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) String filter,
            Model model) {

        if (principal == null) {
            return "redirect:/";
        }

        User user = resolveUser(principal, authToken);

        // Build sort
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy != null && !sortBy.isEmpty() ? sortBy : "dateCreated"
        );

        // Build pageable
        Pageable pageable = PageRequest.of(page, size, sort);

        // Get paginated links with search/filter
        Page<ShortenedUrl> urlPage;
        if (search != null && !search.trim().isEmpty()) {
            urlPage = shortenedUrlService.searchLinks(user, search.trim(), pageable);
        } else if ("expired".equals(filter)) {
            urlPage = shortenedUrlService.findExpiredLinks(user, pageable);
        } else if ("active".equals(filter)) {
            urlPage = shortenedUrlService.findActiveLinks(user, pageable);
        } else {
            urlPage = shortenedUrlService.findByUserPaginated(user, pageable);
        }

        // ===== FIX: Calculate stats with ALL required fields =====
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalLinks", shortenedUrlService.countUserLinks(user));
        stats.put("activeLinks", shortenedUrlService.countActiveLinks(user));
        stats.put("expiredLinks", shortenedUrlService.countExpiredLinks(user));

        // Calculate total clicks from ALL user's links (not just current page)
        List<ShortenedUrl> allUrls = shortenedUrlService.findByUser(user);
        long totalClicks = allUrls.stream()
                .mapToLong(url -> url.getClickCount() != null ? url.getClickCount() : 0)
                .sum();
        stats.put("totalClicks", totalClicks);

        // Count clicks today from ALL user's links
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long clicksToday = allUrls.stream()
                .filter(url -> url.getLastAccessed() != null &&
                        url.getLastAccessed().isAfter(startOfDay))
                .count();
        stats.put("clicksToday", clicksToday);

        model.addAttribute("user", user);
        model.addAttribute("urlPage", urlPage);
        model.addAttribute("stats", stats);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", urlPage.getTotalPages());
        model.addAttribute("search", search);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("filter", filter);

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

        User user = resolveUser(principal, authToken);

        ShortenedUrl url = shortenedUrlService.findByCode(code);

        if (url == null || !url.getUser().getId().equals(user.getId())) {
            return "redirect:/dashboard";
        }

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

        User user = resolveUser(principal, authToken);

        List<ShortenedUrl> urls = shortenedUrlService.findByUser(user);

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

    @GetMapping("/updates")
    public String updates(){
        return "updates";
    }
}