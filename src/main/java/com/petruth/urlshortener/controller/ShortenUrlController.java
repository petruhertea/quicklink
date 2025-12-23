package com.petruth.urlshortener.controller;

import com.petruth.urlshortener.dto.UrlRequest;
import com.petruth.urlshortener.entity.ShortenedUrl;
import com.petruth.urlshortener.entity.User;
import com.petruth.urlshortener.service.ShortenedUrlServiceImpl;
import com.petruth.urlshortener.service.UrlSafetyService;
import com.petruth.urlshortener.service.UserServiceImpl;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
                         UrlSafetyService urlSafetyService){
        this.shortenedUrlService = shortenedUrlService;
        this.userService = userService;
        this.urlSafetyService = urlSafetyService;
    }

    @PostMapping("/shorten")
    public ResponseEntity<?> shortenUrl(
            @Valid @RequestBody UrlRequest request,
            @AuthenticationPrincipal OAuth2User principal) {

        // Check URL safety
        if (!urlSafetyService.isSafeUrl(request.url())) {
            String message = urlSafetyService.getSafetyMessage(request.url());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", message));
        }

        String code = shortenedUrlService.generateUniqueCode();
        ShortenedUrl shortenedUrl = new ShortenedUrl();

        shortenedUrl.setLongUrl(request.url());
        shortenedUrl.setCode(code);
        shortenedUrl.setShortUrl(getBaseUrl() + "/api/" + code);

        // Set expiration if provided
        if (request.expirationDays() != null && request.expirationDays() > 0) {
            shortenedUrl.setExpiresAt(LocalDateTime.now().plusDays(request.expirationDays()));
        }

        // Associate with user if logged in
        if (principal != null) {
            String email = principal.getAttribute("email");
            User user = userService.findByEmail(email)
                    .orElseThrow(()->new RuntimeException("User not found"));
            shortenedUrl.setUser(user);
        }

        shortenedUrlService.save(shortenedUrl);
        return ResponseEntity.ok(Map.of(
                "shortUrl", shortenedUrl.getShortUrl(),
                "code", shortenedUrl.getCode(),
                "expiresAt", shortenedUrl.getExpiresAt() != null ?
                        shortenedUrl.getExpiresAt().toString() : "never"
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