package com.petruth.urlshortener.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class BrevoEmailService {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailService.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.brevo.api-key}")
    private String apiKey;

    @Value("${app.mail.from}")
    private String fromAddress;

    public BrevoEmailService(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    /**
     * Sends an HTML email via the Brevo API.
     * @Async so callers (welcome email, expiry notifications) never block
     * on a slow or failed network call.
     */
    @Async
    public void send(String toEmail, String toName,
                     String subject, String htmlContent) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);

            Map<String, Object> payload = Map.of(
                    "sender",      Map.of("name", "QuickLink", "email", fromAddress),
                    "to",          new Object[]{ Map.of("email", toEmail, "name", toName) },
                    "subject",     subject,
                    "htmlContent", htmlContent
            );

            String body = objectMapper.writeValueAsString(payload);
            HttpEntity<String> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(BREVO_API_URL, HttpMethod.POST,
                            request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Email sent via Brevo to {}", toEmail);
            } else {
                log.error("Brevo returned non-2xx for {}: {}",
                        toEmail, response.getStatusCode());
            }

        } catch (Exception e) {
            // Never propagate — a failed email must not affect the caller
            log.error("Failed to send email to {} via Brevo: {}",
                    toEmail, e.getMessage());
        }
    }
}
