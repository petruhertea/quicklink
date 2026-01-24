package com.petruth.urlshortener.controller;

import com.petruth.urlshortener.dto.BulkUrlRequest;
import com.petruth.urlshortener.dto.BulkUrlResponse;
import com.petruth.urlshortener.dto.UrlRequest;
import com.petruth.urlshortener.entity.ShortenedUrl;
import com.petruth.urlshortener.entity.User;
import com.petruth.urlshortener.entity.UserOAuthProvider;
import com.petruth.urlshortener.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api")
public class ShortenUrlController {

    private final ShortenedUrlServiceImpl shortenedUrlService;
    private final UserServiceImpl userService;
    private final UrlSafetyService urlSafetyService;
    private final AnalyticsService analyticsService;
    private final QRCodeService qrCodeService;

    ShortenUrlController(ShortenedUrlServiceImpl shortenedUrlService,
                         UserServiceImpl userService,
                         UrlSafetyService urlSafetyService,
                         AnalyticsService analyticsService,
                         QRCodeService qrCodeService) {
        this.shortenedUrlService = shortenedUrlService;
        this.userService = userService;
        this.urlSafetyService = urlSafetyService;
        this.analyticsService = analyticsService;
        this.qrCodeService = qrCodeService;
    }

    @GetMapping("/{code}/qrcode")
    public ResponseEntity<byte[]> getQRCode(@PathVariable String code) {
        try {
            ShortenedUrl url = shortenedUrlService.findByCode(code);
            if (url == null) {
                return ResponseEntity.notFound().build();
            }

            byte[] qrCode = qrCodeService.generateQRCode(url.getShortUrl(), 300, 300);

            return ResponseEntity.ok()
                    .header("Content-Type", "image/png")
                    .header("Content-Disposition", "inline; filename=\"qrcode-" + code + ".png\"")
                    .body(qrCode);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/shorten")
    public ResponseEntity<?> shortenUrl(
            @Valid @RequestBody UrlRequest request,
            @AuthenticationPrincipal OAuth2User principal,
            OAuth2AuthenticationToken authToken) {

        // Check URL safety
        if (!urlSafetyService.isSafeUrl(request.url())) {
            String message = urlSafetyService.getSafetyMessage(request.url());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", message));
        }

        String code;
        User user = null;

        // Associate with user if logged in
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
                    throw new RuntimeException("Unknown OAuth provider: " + provider);
                }

                if (oauthId != null) {
                    user = userService.findByOAuth(provider, oauthId)
                            .map(UserOAuthProvider::getUser)
                            .orElse(null);
                }

                if (user == null) {
                    System.err.println("Warning: Authenticated user not found in database. Provider: " + provider + ", OAuthId: " + oauthId);
                }
            } catch (Exception e) {
                System.err.println("Error finding user: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Handle custom code for premium users
        if (request.customCode() != null && !request.customCode().trim().isEmpty()) {
            if (user == null || !user.isPremium()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Custom codes are only available for premium users"));
            }

            code = request.customCode().trim();

            // Check if custom code is already taken
            if (shortenedUrlService.existsByCode(code)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "This custom code is already taken. Please choose another one."));
            }
        } else {
            // Generate random code
            code = shortenedUrlService.generateUniqueCode();
        }

        ShortenedUrl shortenedUrl = new ShortenedUrl();
        shortenedUrl.setLongUrl(request.url());
        shortenedUrl.setCode(code);
        shortenedUrl.setShortUrl(getBaseUrl() + "/api/" + code);

        // Set expiration if provided
        if (request.expirationDays() != null && request.expirationDays() > 0) {
            shortenedUrl.setExpiresAt(LocalDateTime.now().plusDays(request.expirationDays()));
        }

        if (user != null) {
            shortenedUrl.setUser(user);
        }

        // CHANGED: Use createNew() instead of save() for new URLs
        shortenedUrlService.createNew(shortenedUrl);

        return ResponseEntity.ok(Map.of(
                "shortUrl", shortenedUrl.getShortUrl(),
                "code", shortenedUrl.getCode(),
                "expiresAt", shortenedUrl.getExpiresAt() != null ?
                        shortenedUrl.getExpiresAt().toString() : "never",
                "customCode", request.customCode() != null
        ));
    }

    @PostMapping("/bulk-shorten")
    public ResponseEntity<?> bulkShortenUrls(
            @Valid @RequestBody BulkUrlRequest request,
            @AuthenticationPrincipal OAuth2User principal,
            OAuth2AuthenticationToken authToken) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }

        // Get user
        String provider = authToken.getAuthorizedClientRegistrationId();
        String oauthId;
        if ("google".equals(provider)) {
            oauthId = principal.getAttribute("sub");
        } else if ("github".equals(provider)) {
            Object idObj = principal.getAttribute("id");
            oauthId = (idObj != null) ? idObj.toString() : null;
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User user = userService.findByOAuth(provider, oauthId)
                .map(oauthProvider -> oauthProvider.getUser())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "User not found"));
        }

        // Check limits based on user tier
        int maxBulk = user.isPremium() ? 100 : 10;
        if (request.urls().size() > maxBulk) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Maximum " + maxBulk + " URLs allowed. Upgrade to Premium for 100 URLs."));
        }

        // Process URLs
        List<BulkUrlResponse.UrlResult> results = new ArrayList<>();
        int successful = 0;
        int failed = 0;

        for (UrlRequest urlReq : request.urls()) {
            try {
                // Validate URL
                if (urlReq.url() == null || urlReq.url().trim().isEmpty()) {
                    results.add(new BulkUrlResponse.UrlResult(
                            urlReq.url(), null, null, false, "URL is required"
                    ));
                    failed++;
                    continue;
                }

                if (!urlSafetyService.isSafeUrl(urlReq.url())) {
                    results.add(new BulkUrlResponse.UrlResult(
                            urlReq.url(), null, null, false,
                            urlSafetyService.getSafetyMessage(urlReq.url())
                    ));
                    failed++;
                    continue;
                }

                // Generate or use custom code
                String code;
                if (urlReq.customCode() != null && !urlReq.customCode().trim().isEmpty()) {
                    if (!user.isPremium()) {
                        results.add(new BulkUrlResponse.UrlResult(
                                urlReq.url(), null, null, false,
                                "Custom codes require Premium"
                        ));
                        failed++;
                        continue;
                    }

                    code = urlReq.customCode().trim();
                    if (shortenedUrlService.existsByCode(code)) {
                        results.add(new BulkUrlResponse.UrlResult(
                                urlReq.url(), null, null, false,
                                "Code '" + code + "' already taken"
                        ));
                        failed++;
                        continue;
                    }
                } else {
                    code = shortenedUrlService.generateUniqueCode();
                }

                // Create shortened URL
                ShortenedUrl shortenedUrl = new ShortenedUrl();
                shortenedUrl.setLongUrl(urlReq.url());
                shortenedUrl.setCode(code);
                shortenedUrl.setShortUrl(getBaseUrl() + "/api/" + code);
                shortenedUrl.setUser(user);

                // Set expiration if provided
                if (urlReq.expirationDays() != null && urlReq.expirationDays() > 0) {
                    shortenedUrl.setExpiresAt(LocalDateTime.now().plusDays(urlReq.expirationDays()));
                }

                // CHANGED: Use createNew() for bulk operations
                shortenedUrlService.createNew(shortenedUrl);

                results.add(new BulkUrlResponse.UrlResult(
                        urlReq.url(), shortenedUrl.getShortUrl(), code, true, null
                ));
                successful++;

            } catch (Exception e) {
                results.add(new BulkUrlResponse.UrlResult(
                        urlReq.url(), null, null, false,
                        "Failed to create: " + e.getMessage()
                ));
                failed++;
            }
        }

        BulkUrlResponse response = new BulkUrlResponse(
                request.urls().size(), successful, failed, results
        );

        return ResponseEntity.ok(response);
    }

    /**
     * OPTIMIZED: Redirect endpoint now uses direct SQL increment
     * No more cache invalidation on every click!
     */
    @GetMapping("/{code}")
    public ResponseEntity<?> redirectToLongUrl(@PathVariable String code, HttpServletRequest request) {
        // Use cached version for lookup
        ShortenedUrl shortenedUrl = shortenedUrlService.findByCodeForRedirect(code);

        if (shortenedUrl == null) {
            return ResponseEntity.notFound().build();
        }

        // Check expiration
        if (shortenedUrl.getExpiresAt() != null &&
                LocalDateTime.now().isAfter(shortenedUrl.getExpiresAt())) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body("<html><body><h1>410 - Link Expired</h1><p>This shortened link has expired and is no longer available.</p></body></html>");
        }

        // OPTIMIZED: Use direct SQL update - doesn't invalidate cache
        shortenedUrlService.incrementClickCount(code);

        // Record detailed analytics (async recommended for production)
        analyticsService.recordClick(shortenedUrl, request);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(shortenedUrl.getLongUrl()))
                .build();
    }

    @GetMapping("/analytics/{code}")
    public ResponseEntity<?> getAnalytics(
            @PathVariable String code,
            @RequestParam(defaultValue = "30") int days,
            @AuthenticationPrincipal OAuth2User principal,
            OAuth2AuthenticationToken authToken) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ShortenedUrl url = shortenedUrlService.findByCode(code);
        if (url == null) {
            return ResponseEntity.notFound().build();
        }

        // Verify user owns this URL
        String provider = authToken.getAuthorizedClientRegistrationId();
        String oauthId;
        if ("google".equals(provider)) {
            oauthId = principal.getAttribute("sub");
        } else if ("github".equals(provider)) {
            Object idObj = principal.getAttribute("id");
            oauthId = (idObj != null) ? idObj.toString() : null;
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User user = userService.findByOAuth(provider, oauthId)
                .map(oauthProvider -> oauthProvider.getUser())
                .orElse(null);

        if (user == null || !url.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Get analytics
        Map<String, Object> analytics = analyticsService.getAnalyticsForUrl(url, days);

        return ResponseEntity.ok(analytics);
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<?> deleteLink(
            @PathVariable String code,
            @AuthenticationPrincipal OAuth2User principal,
            OAuth2AuthenticationToken authToken) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ShortenedUrl url = shortenedUrlService.findByCode(code);
        if (url == null) {
            return ResponseEntity.notFound().build();
        }

        // Verify user owns this URL
        String provider = authToken.getAuthorizedClientRegistrationId();
        String oauthId;
        if ("google".equals(provider)) {
            oauthId = principal.getAttribute("sub");
        } else if ("github".equals(provider)) {
            Object idObj = principal.getAttribute("id");
            oauthId = (idObj != null) ? idObj.toString() : null;
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User user = userService.findByOAuth(provider, oauthId)
                .map(oauthProvider -> oauthProvider.getUser())
                .orElse(null);

        if (user == null || url.getUser() == null || !url.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have permission to delete this link"));
        }

        shortenedUrlService.delete(url);

        return ResponseEntity.ok(Map.of("message", "Link deleted successfully"));
    }

    public String getBaseUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
}