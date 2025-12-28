package com.petruth.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import org.hibernate.validator.constraints.URL;

public record UrlRequest(
        @URL(message = "Invalid URL format")
        @NotBlank(message = "The url must not be empty")
        @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
        @Size(max = 2048, message = "URL is too long")
        String url,

        @Min(value = 0, message = "Expiration days must be 0 or positive")
        @Max(value = 365, message = "Expiration days cannot exceed 365")
        Integer expirationDays,

        @Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "Custom code can only contain letters, numbers, hyphens and underscores")
        @Size(min = 3, max = 20, message = "Custom code must be between 3 and 20 characters")
        String customCode  // Only for premium users
) {}