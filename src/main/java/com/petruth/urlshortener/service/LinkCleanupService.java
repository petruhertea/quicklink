package com.petruth.urlshortener.service;

import com.petruth.urlshortener.repository.ShortenedUrlRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LinkCleanupService {

    private final ShortenedUrlRepository shortenedUrlRepository;

    public LinkCleanupService(ShortenedUrlRepository shortenedUrlRepository) {
        this.shortenedUrlRepository = shortenedUrlRepository;
    }

    // Run every day at 3 AM
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void deleteExpiredLinks() {
        LocalDateTime now = LocalDateTime.now();
        // Delete links that expired more than 7 days ago
        LocalDateTime cutoff = now.minusDays(7);

        long deleted = shortenedUrlRepository.deleteByExpiresAtBefore(cutoff);

        if (deleted > 0) {
            System.out.println("Deleted " + deleted + " expired links");
        }
    }
}