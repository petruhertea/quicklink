package com.petruth.urlshortener.service;

import com.petruth.urlshortener.entity.ShortenedUrl;
import com.petruth.urlshortener.entity.User;

import java.util.List;

public interface ShortenedUrlService {
    String generateUniqueCode();
    ShortenedUrl save(ShortenedUrl shortenedUrl);
    ShortenedUrl findByCode(String code);
    List<ShortenedUrl> findByUser(User user);
    boolean existsByCode(String code);
}
