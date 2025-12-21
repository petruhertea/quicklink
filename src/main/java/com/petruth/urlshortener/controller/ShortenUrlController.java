package com.petruth.urlshortener.controller;

import com.petruth.urlshortener.dto.UrlRequest;
import com.petruth.urlshortener.entity.ShortenedUrl;
import com.petruth.urlshortener.entity.User;
import com.petruth.urlshortener.service.ShortenedUrlServiceImpl;
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

@Validated
@RestController
@RequestMapping("/api")
public class ShortenUrlController {

    private final ShortenedUrlServiceImpl shortenedUrlService;
    private final UserServiceImpl userService;

    ShortenUrlController(ShortenedUrlServiceImpl shortenedUrlService, UserServiceImpl userService){
        this.shortenedUrlService = shortenedUrlService;
        this.userService = userService;
    }

    @PostMapping("/shorten")
    public ResponseEntity<?> shortenUrl(
            @Valid @RequestBody UrlRequest request,
            @AuthenticationPrincipal OAuth2User principal) {

        String code = shortenedUrlService.generateUniqueCode();
        ShortenedUrl shortenedUrl = new ShortenedUrl();

        shortenedUrl.setLongUrl(request.url());
        shortenedUrl.setCode(code);
        shortenedUrl.setShortUrl(getBaseUrl() + "/api/" + code);

        // Associate with user if logged in
        if (principal != null) {
            String email = principal.getAttribute("email");
            User user = userService.findByEmail(email).orElseThrow(()->new RuntimeException("User not found"));
            shortenedUrl.setUser(user);
        }

        shortenedUrlService.save(shortenedUrl);
        return ResponseEntity.ok(shortenedUrl.getShortUrl());
    }

    @GetMapping("/{code}")
    public ResponseEntity<?> redirectToLongUrl(@PathVariable String code) {
        ShortenedUrl shortenedUrl = shortenedUrlService.findByCode(code);

        if (shortenedUrl == null) return ResponseEntity.notFound().build();

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
