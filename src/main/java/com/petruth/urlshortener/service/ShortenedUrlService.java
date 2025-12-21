package com.petruth.urlshortener.service;

import com.petruth.urlshortener.entity.ShortenedUrl;

public interface ShortenedUrlService {
    String generateUniqueCode();
    ShortenedUrl save(ShortenedUrl shortenedUrl);
    ShortenedUrl findByCode(String code);
}
