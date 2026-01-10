package com.petruth.urlshortener.dto;

import java.util.List;

public record BulkUrlResponse(
        int total,
        int successful,
        int failed,
        List<UrlResult> results
) {
    public record UrlResult(
            String originalUrl,
            String shortUrl,
            String code,
            boolean success,
            String error
    ) {}
}