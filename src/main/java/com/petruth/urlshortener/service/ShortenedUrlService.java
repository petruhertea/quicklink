package com.petruth.urlshortener.service;

import com.petruth.urlshortener.dto.LinkSearchRequest;
import com.petruth.urlshortener.entity.ShortenedUrl;
import com.petruth.urlshortener.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ShortenedUrlService {
    String generateUniqueCode();
    ShortenedUrl save(ShortenedUrl shortenedUrl);
    ShortenedUrl findByCode(String code);
    List<ShortenedUrl> findByUser(User user);
    boolean existsByCode(String code);
    void delete(ShortenedUrl url);

    Page<ShortenedUrl> findByUserPaginated(User user, Pageable pageable);

    Page<ShortenedUrl> searchLinks(User user, String searchTerm, Pageable pageable);

    Page<ShortenedUrl> advancedSearchLinks(User user, LinkSearchRequest request, Pageable pageable);

    Page<ShortenedUrl> findExpiredLinks(User user, Pageable pageable);

    Page<ShortenedUrl> findActiveLinks(User user, Pageable pageable);

    long countUserLinks(User user);
    long countExpiredLinks(User user);
    long countActiveLinks(User user);
}
