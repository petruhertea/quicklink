package com.petruth.urlshortener.controller;

import com.petruth.urlshortener.dto.UrlRequest;
import com.petruth.urlshortener.entity.ShortenedUrl;
import com.petruth.urlshortener.entity.User;
import com.petruth.urlshortener.entity.UserOAuthProvider;
import com.petruth.urlshortener.service.ShortenedUrlServiceImpl;
import com.petruth.urlshortener.service.UrlSafetyService;
import com.petruth.urlshortener.service.UserServiceImpl;
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
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api")
public class ShortenUrlController {

    private final ShortenedUrlServiceImpl shortenedUrlService;
    private final UserServiceImpl userService;
    private final UrlSafetyService urlSafetyService;

    ShortenUrlController(ShortenedUrlServiceImpl shortenedUrlService,
                         UserServiceImpl userService,
                         UrlSafetyService urlSafetyService) {
        this.shortenedUrlService = shortenedUrlService;
        this.userService = userService;
        this.urlSafetyService = urlSafetyService;
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

        shortenedUrlService.save(shortenedUrl);

        return ResponseEntity.ok(Map.of(
                "shortUrl", shortenedUrl.getShortUrl(),
                "code", shortenedUrl.getCode(),
                "expiresAt", shortenedUrl.getExpiresAt() != null ?
                        shortenedUrl.getExpiresAt().toString() : "never",
                "customCode", request.customCode() != null
        ));
    }

    @GetMapping("/{code}")
    public ResponseEntity<?> redirectToLongUrl(@PathVariable String code) {
        ShortenedUrl shortenedUrl = shortenedUrlService.findByCode(code);

        if (shortenedUrl == null) {
            return ResponseEntity.notFound().build();
        }

        // Check expiration
        if (shortenedUrl.getExpiresAt() != null &&
                LocalDateTime.now().isAfter(shortenedUrl.getExpiresAt())) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body("<html><body><h1>410 - Link Expired</h1><p>This shortened link has expired and is no longer available.</p></body></html>");
        }

        // Track analytics
        shortenedUrl.setClickCount(shortenedUrl.getClickCount() + 1);
        shortenedUrl.setLastAccessed(LocalDateTime.now());
        shortenedUrlService.save(shortenedUrl);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(shortenedUrl.getLongUrl()))
                .build();
    }

    public String getBaseUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
}