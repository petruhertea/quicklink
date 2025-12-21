package com.petruth.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record UrlRequest(
        @URL(message = "Invalid URL format")
        @NotBlank(message = "The url must not be empty")
        String url
){}
