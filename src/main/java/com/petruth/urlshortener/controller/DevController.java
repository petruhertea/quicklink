package com.petruth.urlshortener.controller;

import com.petruth.urlshortener.service.LinkExpiryNotificationService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/dev")
@Profile("dev") // only exists when spring.profiles.active=dev
public class DevController {

    private final LinkExpiryNotificationService notificationService;

    public DevController(LinkExpiryNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/trigger-expiry-notifications")
    public ResponseEntity<?> triggerNotifications() {
        notificationService.sendExpiryNotifications();
        return ResponseEntity.ok(Map.of("message", "Notification job triggered"));
    }
}
