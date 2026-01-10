package com.petruth.urlshortener.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkUrlRequest(
        @NotEmpty(message = "URL list cannot be empty")
        @Size(max = 100, message = "Maximum 100 URLs at once")
        List<SingleUrlRequest> urls
) {
    public record SingleUrlRequest(
            String url,
            String customCode,
            Integer expirationDays
    ) {}
}