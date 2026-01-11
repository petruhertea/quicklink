package com.petruth.urlshortener.service;

import com.petruth.urlshortener.dto.LinkSearchRequest;
import com.petruth.urlshortener.entity.ShortenedUrl;
import com.petruth.urlshortener.entity.User;
import com.petruth.urlshortener.repository.ShortenedUrlRepository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@CacheConfig(cacheNames = "urls")
public class ShortenedUrlServiceImpl implements ShortenedUrlService {

    private final ShortenedUrlRepository shortenedUrlRepository;
    private final int numberOfCharacters = 7;
    private final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private final Random random = new Random();

    public ShortenedUrlServiceImpl(ShortenedUrlRepository shortenedUrlRepository) {
        this.shortenedUrlRepository = shortenedUrlRepository;
    }

    @Override
    public String generateUniqueCode() {
        char[] codeChars = new char[numberOfCharacters];
        String code;

        do {
            for (int i = 0; i < numberOfCharacters; i++) {
                int randomIndex = random.nextInt(alphabet.length());
                codeChars[i] = alphabet.charAt(randomIndex);
            }
            code = new String(codeChars);
        } while (shortenedUrlRepository.existsByCode(code));

        return code;
    }

    @Override
    @CacheEvict(allEntries = true)
    public ShortenedUrl save(ShortenedUrl shortenedUrl) {
        return shortenedUrlRepository.save(shortenedUrl);
    }

    @Override
    @Cacheable(value = "urls", key = "#code")
    public ShortenedUrl findByCode(String code) {
        return shortenedUrlRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("URL with code: " + code + " not found"));
    }

    @Override
    public List<ShortenedUrl> findByUser(User user) {
        return shortenedUrlRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("URLs not found"));
    }

    @Override
    public boolean existsByCode(String code) {
        return shortenedUrlRepository.existsByCode(code);
    }

    @Override
    @CacheEvict(allEntries = true)
    public void delete(ShortenedUrl url) {
        shortenedUrlRepository.delete(url);
    }

    // ===== PAGINATION & SEARCH IMPLEMENTATION =====

    @Override
    public Page<ShortenedUrl> findByUserPaginated(User user, Pageable pageable) {
        return shortenedUrlRepository.findByUser(user, pageable);
    }

    @Override
    public Page<ShortenedUrl> searchLinks(User user, String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findByUserPaginated(user, pageable);
        }

        // Use the custom @Query method for combined search
        return shortenedUrlRepository.searchByTerm(user, searchTerm, pageable);
    }

    @Override
    public Page<ShortenedUrl> advancedSearchLinks(User user, LinkSearchRequest request, Pageable pageable) {
        // Use custom @Query for advanced search with multiple filters
        return shortenedUrlRepository.advancedSearch(
                user,
                request.searchTerm(),
                request.startDate(),
                request.endDate(),
                request.minClicks(),
                request.maxClicks(),
                pageable
        );
    }

    @Override
    public Page<ShortenedUrl> findExpiredLinks(User user, Pageable pageable) {
        // Use JPA method name - finds links expired before now
        return shortenedUrlRepository.findByUserAndExpiresAtBefore(user, LocalDateTime.now(), pageable);
    }

    @Override
    public Page<ShortenedUrl> findActiveLinks(User user, Pageable pageable) {
        // Active links = not expired OR no expiration date
        // We need to combine two queries here
        LocalDateTime now = LocalDateTime.now();

        // Option 1: Links that expire in the future
        Page<ShortenedUrl> notExpired = shortenedUrlRepository.findByUserAndExpiresAtAfter(user, now, pageable);

        // Option 2: Links with no expiration
        // In a real scenario, you'd want to combine these results
        // For simplicity, let's use a custom query instead

        // Better approach: Use advanced search
        LinkSearchRequest request = new LinkSearchRequest(
                null, null, null, null, null, false, "dateCreated", "desc"
        );
        return advancedSearchLinks(user, request, pageable);
    }

    @Override
    public long countUserLinks(User user) {
        return shortenedUrlRepository.countByUser(user);
    }

    @Override
    public long countExpiredLinks(User user) {
        return shortenedUrlRepository.countByUserAndExpiresAtBefore(user, LocalDateTime.now());
    }

    @Override
    public long countActiveLinks(User user) {
        // Active = (expires in future) + (no expiration)
        long futureExpiration = shortenedUrlRepository.countByUserAndExpiresAtAfter(user, LocalDateTime.now());
        long noExpiration = shortenedUrlRepository.countByUserAndExpiresAtIsNull(user);
        return futureExpiration + noExpiration;
    }
}