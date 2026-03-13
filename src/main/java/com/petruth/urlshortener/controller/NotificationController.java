package com.petruth.urlshortener.controller;

import com.petruth.urlshortener.entity.ShortenedUrl;
import com.petruth.urlshortener.entity.User;
import com.petruth.urlshortener.entity.UserOAuthProvider;
import com.petruth.urlshortener.service.ExtensionTokenService;
import com.petruth.urlshortener.service.ShortenedUrlService;
import com.petruth.urlshortener.service.UserServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Controller
public class NotificationController {

    private final UserServiceImpl userService;
    private final ShortenedUrlService shortenedUrlService;
    private final ExtensionTokenService tokenService;

    public NotificationController(UserServiceImpl userService,
                                  ShortenedUrlService shortenedUrlService,
                                  ExtensionTokenService tokenService) {
        this.userService   = userService;
        this.shortenedUrlService = shortenedUrlService;
        this.tokenService  = tokenService;
    }

    /**
     * Toggle notification preference — called via fetch() from the dashboard.
     */
    @PostMapping("/api/notifications/preference")
    @ResponseBody
    public ResponseEntity<?> updatePreference(
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal OAuth2User principal,
            OAuth2AuthenticationToken authToken) {

        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = resolveUser(principal, authToken);
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));

        user.setNotifyExpiringLinks(enabled);
        user.setNotificationPromptShown(true);
        userService.save(user);

        return ResponseEntity.ok(Map.of(
                "notifyExpiringLinks", user.isNotifyExpiringLinks()));
    }

    /**
     * Dismiss the contextual prompt without enabling notifications.
     */
    @PostMapping("/api/notifications/dismiss-prompt")
    @ResponseBody
    public ResponseEntity<?> dismissPrompt(
            @AuthenticationPrincipal OAuth2User principal,
            OAuth2AuthenticationToken authToken) {

        if (principal == null) return ResponseEntity.status(401).build();

        User user = resolveUser(principal, authToken);
        user.setNotificationPromptShown(true);
        userService.save(user);

        return ResponseEntity.ok().build();
    }

    /**
     * One-click extension from email — no login required.
     * The token encodes urlId, userId, and expiry; it's HMAC-signed.
     */
    @GetMapping("/links/extend")
    public String extendLink(@RequestParam String token) {
        Optional<Long> urlIdOpt = tokenService.validateToken(token);

        if (urlIdOpt.isEmpty()) {
            return "redirect:/?extend=invalid";
        }

        ShortenedUrl url = shortenedUrlService.findById(urlIdOpt.get()).orElse(null);
        if (url == null) {
            return "redirect:/?extend=notfound";
        }

        LocalDateTime newExpiry = (url.getExpiresAt() != null)
                ? url.getExpiresAt().plusDays(30)
                : LocalDateTime.now().plusDays(30);

        url.setExpiresAt(newExpiry);
        url.setExpiryNotificationSentAt(null);
        shortenedUrlService.save(url); // @CachePut updates the cache entry by code

        return "redirect:/dashboard?extend=success&code=" + url.getCode();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private User resolveUser(OAuth2User principal, OAuth2AuthenticationToken token) {
        String provider = token.getAuthorizedClientRegistrationId();
        String oauthId;

        if ("google".equals(provider)) {
            oauthId = principal.getAttribute("sub");
        } else if ("github".equals(provider)) {
            Object id = principal.getAttribute("id");
            oauthId = id != null ? id.toString() : null;
        } else {
            oauthId = principal.getAttribute("oid");
            if (oauthId == null) oauthId = principal.getAttribute("sub");
        }

        return userService.findByOAuth(provider, oauthId)
                .map(UserOAuthProvider::getUser)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}