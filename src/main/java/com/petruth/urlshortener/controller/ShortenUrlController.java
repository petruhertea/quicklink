package com.petruth.urlshortener.controller;

import com.petruth.urlshortener.dto.UrlRequest;
import com.petruth.urlshortener.entity.ShortenedUrl;
import com.petruth.urlshortener.service.ShortenedUrlServiceImpl;
import jakarta.validation.Valid;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;

@Validated
@RestController
@RequestMapping("/api")
public class ShortenUrlController {

    private final ShortenedUrlServiceImpl shortenedUrlService;

    ShortenUrlController(ShortenedUrlServiceImpl shortenedUrlService){
        this.shortenedUrlService = shortenedUrlService;
    }

    @PostMapping("/shorten")
    public ResponseEntity<?> shortenUrl(@Valid @RequestBody UrlRequest request){
        String code = shortenedUrlService.generateUniqueCode();

        ShortenedUrl shortenedUrl = new ShortenedUrl();

        shortenedUrl.setLongUrl(request.url());
        shortenedUrl.setCode(code);
        shortenedUrl.setShortUrl(getBaseUrl()+"/api/"+code);
        shortenedUrl.setDateCreated(LocalDateTime.now());

        shortenedUrlService.save(shortenedUrl);

        return ResponseEntity.ok(shortenedUrl.getShortUrl());
    }

    @GetMapping("/{code}")
    public ResponseEntity<?> redirectToLongUrl(@PathVariable String code){
        ShortenedUrl shortenedUrl = shortenedUrlService.findByCode(code);

        if (shortenedUrl == null) return ResponseEntity.notFound().build();

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(shortenedUrl.getLongUrl())).build();
    }

    public String getBaseUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
}
