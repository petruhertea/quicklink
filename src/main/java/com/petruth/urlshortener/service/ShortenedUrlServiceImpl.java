package com.petruth.urlshortener.service;

import com.petruth.urlshortener.dto.LinkSearchRequest;
import com.petruth.urlshortener.entity.ShortenedUrl;
import com.petruth.urlshortener.entity.User;
import com.petruth.urlshortener.repository.ShortenedUrlRepository;
import com.petruth.urlshortener.repository.ShortenedUrlSpecifications;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * CHANGED: Use @CachePut instead of @CacheEvict for updates
     * This updates the cache entry instead of clearing everything
     */
    @Override
    @CachePut(value = "urls", key = "#shortenedUrl.code")
    public ShortenedUrl save(ShortenedUrl shortenedUrl) {
        return shortenedUrlRepository.save(shortenedUrl);
    }

    /**
     * NEW: Special method for creating new URLs (clears cache)
     */
    @Override
    @CacheEvict(value = "urls", allEntries = true)
    public ShortenedUrl createNew(ShortenedUrl shortenedUrl) {
        return shortenedUrlRepository.save(shortenedUrl);
    }

    @Override
    @Cacheable(value = "urls", key = "#code")
    public ShortenedUrl findByCode(String code) {
        return shortenedUrlRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("URL with code: " + code + " not found"));
    }

    /**
     * NEW: Optimized method for incrementing clicks WITHOUT full entity update
     * Uses direct SQL update - no cache invalidation needed
     */
    @Override
    @Transactional
    public void incrementClickCount(String code) {
        shortenedUrlRepository.incrementClickCount(code, LocalDateTime.now());
    }

    /**
     * NEW: Get URL for redirect (doesn't increment in service layer)
     * Controller handles the increment separately
     */
    @Override
    @Cacheable(value = "urls", key = "#code")
    public ShortenedUrl findByCodeForRedirect(String code) {
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

    /**
     * CHANGED: Use @CacheEvict with specific key instead of allEntries
     */
    @Override
    @CacheEvict(value = "urls", key = "#url.code")
    public void delete(ShortenedUrl url) {
        shortenedUrlRepository.delete(url);
    }

    // ===== PAGINATION METHODS (No cache needed - these are queries) =====

    @Override
    public Page<ShortenedUrl> findByUserPaginated(User user, Pageable pageable) {
        return shortenedUrlRepository.findAll(
                ShortenedUrlSpecifications.belongsToUser(user),
                pageable
        );
    }

    @Override
    public Page<ShortenedUrl> searchLinks(User user, String searchTerm, Pageable pageable) {
        Specification<ShortenedUrl> spec = Specification
                .where(ShortenedUrlSpecifications.belongsToUser(user))
                .and(ShortenedUrlSpecifications.searchByTerm(searchTerm));

        return shortenedUrlRepository.findAll(spec, pageable);
    }

    @Override
    public Page<ShortenedUrl> advancedSearchLinks(User user, LinkSearchRequest request, Pageable pageable) {
        Specification<ShortenedUrl> spec = ShortenedUrlSpecifications.advancedSearch(
                user,
                request.searchTerm(),
                request.startDate(),
                request.endDate(),
                request.minClicks(),
                request.maxClicks(),
                request.expired()
        );

        return shortenedUrlRepository.findAll(spec, pageable);
    }

    @Override
    public Page<ShortenedUrl> findExpiredLinks(User user, Pageable pageable) {
        Specification<ShortenedUrl> spec = Specification
                .where(ShortenedUrlSpecifications.belongsToUser(user))
                .and(ShortenedUrlSpecifications.expiredLinks());

        return shortenedUrlRepository.findAll(spec, pageable);
    }

    @Override
    public Page<ShortenedUrl> findActiveLinks(User user, Pageable pageable) {
        Specification<ShortenedUrl> spec = Specification
                .where(ShortenedUrlSpecifications.belongsToUser(user))
                .and(ShortenedUrlSpecifications.activeLinks());

        return shortenedUrlRepository.findAll(spec, pageable);
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
        return shortenedUrlRepository.countByUserAndExpiresAtAfter(user, LocalDateTime.now());
    }
}