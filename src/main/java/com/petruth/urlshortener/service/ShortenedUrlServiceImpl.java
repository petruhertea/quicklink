package com.petruth.urlshortener.service;

import com.petruth.urlshortener.entity.ShortenedUrl;
import com.petruth.urlshortener.entity.User;
import com.petruth.urlshortener.repository.ShortenedUrlRepository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@CacheConfig(cacheNames = "urls")
public class ShortenedUrlServiceImpl implements ShortenedUrlService{
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
                int randomIndex = random.nextInt(alphabet.length() - 1);
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
        return shortenedUrlRepository.findByCode(code).orElseThrow(() -> new RuntimeException("URL with code: " + code + " not found"));
    }

    @Override
    public List<ShortenedUrl> findByUser(User user){
        return shortenedUrlRepository.findByUser(user).orElseThrow(()->new RuntimeException("URLs not found"));
    }
}
