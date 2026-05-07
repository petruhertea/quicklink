package com.petruth.urlshortener.dto;

public record ClickContext(
        String ipAddress,
        String userAgent,
        String referer
) {}
