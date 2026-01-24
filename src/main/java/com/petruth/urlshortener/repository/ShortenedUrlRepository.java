package com.petruth.urlshortener.repository;

import com.petruth.urlshortener.entity.ShortenedUrl;
import com.petruth.urlshortener.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShortenedUrlRepository extends JpaRepository<ShortenedUrl, Long>, JpaSpecificationExecutor<ShortenedUrl> {

    // ===== EXISTING METHODS =====
    boolean existsByCode(String code);
    Optional<ShortenedUrl> findByCode(String code);
    Optional<List<ShortenedUrl>> findByUser(User user);

    @Modifying
    @Query("DELETE FROM ShortenedUrl s WHERE s.expiresAt < :cutoffDate")
    long deleteByExpiresAtBefore(LocalDateTime cutoffDate);

    @Modifying
    @Query("UPDATE ShortenedUrl s SET s.clickCount = s.clickCount + 1, s.lastAccessed = :now WHERE s.code = :code")
    void incrementClickCount(String code, LocalDateTime now);

    // ===== NEW PAGINATION METHODS =====

    // Basic pagination - Spring Data JPA auto-implements this!
    Page<ShortenedUrl> findByUser(User user, Pageable pageable);

    // ===== SIMPLE JPA METHOD NAMES (Auto-implemented) =====

    // Find expired links
    Page<ShortenedUrl> findByUserAndExpiresAtBefore(
            User user, LocalDateTime date, Pageable pageable);

    // Find active links (not expired)
    Page<ShortenedUrl> findByUserAndExpiresAtAfter(
            User user, LocalDateTime date, Pageable pageable);

    // Find links with no expiration
    Page<ShortenedUrl> findByUserAndExpiresAtIsNull(
            User user, Pageable pageable);

    // Count methods - no need for custom queries
    long countByUser(User user);
    long countByUserAndExpiresAtBefore(User user, LocalDateTime date);
    long countByUserAndExpiresAtAfter(User user, LocalDateTime date);
    long countByUserAndExpiresAtIsNull(User user);

    // Add paginated version
    Page<ShortenedUrl> findAll(Specification<ShortenedUrl> spec, Pageable pageable);
}