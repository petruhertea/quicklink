package com.petruth.urlshortener.repository;

import com.petruth.urlshortener.entity.ShortenedUrl;
import com.petruth.urlshortener.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShortenedUrlRepository extends JpaRepository<ShortenedUrl, Long> {

    // ===== EXISTING METHODS =====
    boolean existsByCode(String code);
    Optional<ShortenedUrl> findByCode(String code);
    Optional<List<ShortenedUrl>> findByUser(User user);

    @Modifying
    @Query("DELETE FROM ShortenedUrl s WHERE s.expiresAt < :cutoffDate")
    long deleteByExpiresAtBefore(LocalDateTime cutoffDate);

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

    // ===== CUSTOM @Query METHODS (FIXED FOR POSTGRESQL) =====

    // Combined search (code OR URL) - FIXED: Added CAST to handle bytea issue
    @Query("SELECT s FROM ShortenedUrl s WHERE s.user = :user " +
            "AND (LOWER(CAST(s.code AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(CAST(s.longUrl AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<ShortenedUrl> searchByTerm(
            @Param("user") User user,
            @Param("searchTerm") String searchTerm,
            Pageable pageable);

    // Advanced multi-filter search - FIXED: Added CAST to handle bytea issue
    @Query("SELECT s FROM ShortenedUrl s WHERE s.user = :user " +
            "AND (:searchTerm IS NULL OR " +
            "    LOWER(CAST(s.code AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "    LOWER(CAST(s.longUrl AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "AND (:startDate IS NULL OR s.dateCreated >= :startDate) " +
            "AND (:endDate IS NULL OR s.dateCreated <= :endDate) " +
            "AND (:minClicks IS NULL OR s.clickCount >= :minClicks) " +
            "AND (:maxClicks IS NULL OR s.clickCount <= :maxClicks)")
    Page<ShortenedUrl> advancedSearch(
            @Param("user") User user,
            @Param("searchTerm") String searchTerm,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("minClicks") Long minClicks,
            @Param("maxClicks") Long maxClicks,
            Pageable pageable
    );
}