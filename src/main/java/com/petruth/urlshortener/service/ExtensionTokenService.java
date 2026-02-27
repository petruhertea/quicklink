package com.petruth.urlshortener.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class ExtensionTokenService {

    // Reuses APP_URL secret as HMAC key — add a dedicated secret in production
    @Value("${app.extension.token.secret:changeme-use-env-var}")
    private String secret;

    // Tokens are valid for 7 days
    private static final long TOKEN_TTL_SECONDS = 7 * 24 * 3600;

    /**
     * Produces a URL-safe token encoding: urlId|userId|expiry|signature
     */
    public String generateToken(Long urlId, Long userId) {
        long expiry = Instant.now().getEpochSecond() + TOKEN_TTL_SECONDS;
        String payload = urlId + "|" + userId + "|" + expiry;
        String signature = sign(payload);
        String raw = payload + "|" + signature;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns the urlId if the token is valid and unexpired, otherwise empty.
     */
    public java.util.Optional<Long> validateToken(String token) {
        try {
            String raw = new String(
                    Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|");
            if (parts.length != 4) return java.util.Optional.empty();

            long urlId  = Long.parseLong(parts[0]);
            long userId = Long.parseLong(parts[1]);
            long expiry = Long.parseLong(parts[2]);
            String sig  = parts[3];

            if (Instant.now().getEpochSecond() > expiry) return java.util.Optional.empty();

            String expectedSig = sign(parts[0] + "|" + parts[1] + "|" + parts[2]);
            if (!sig.equals(expectedSig)) return java.util.Optional.empty();

            return java.util.Optional.of(urlId);
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Token signing failed", e);
        }
    }
}
