package com.petruth.urlshortener.repository;

import com.petruth.urlshortener.entity.ShortenedUrl;
import com.petruth.urlshortener.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShortenedUrlRepository extends JpaRepository<ShortenedUrl, Long> {
    boolean existsByCode(String code);
    Optional<ShortenedUrl> findByCode(String code);
    Optional<List<ShortenedUrl>> findByUser(User user);
    @Modifying
    @Query("DELETE FROM ShortenedUrl s WHERE s.expiresAt < :cutoffDate")
    long deleteByExpiresAtBefore(LocalDateTime cutoffDate);
}
