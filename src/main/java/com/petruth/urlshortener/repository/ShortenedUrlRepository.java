package com.petruth.urlshortener.repository;

import com.petruth.urlshortener.entity.ShortenedUrl;
import com.petruth.urlshortener.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShortenedUrlRepository extends JpaRepository<ShortenedUrl, Long> {
    boolean existsByCode(String code);
    Optional<ShortenedUrl> findByCode(String code);
    Optional<List<ShortenedUrl>> findByUser(User user);
}
