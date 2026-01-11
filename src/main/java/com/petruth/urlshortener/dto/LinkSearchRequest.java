package com.petruth.urlshortener.dto;

import java.time.LocalDateTime;

public record LinkSearchRequest(
        String searchTerm,        // Search in URL, code
        LocalDateTime startDate,  // Filter by creation date
        LocalDateTime endDate,
        Long minClicks,          // Filter by click count
        Long maxClicks,
        Boolean expired,         // Show only expired/non-expired
        String sortBy,           // code, dateCreated, clickCount, lastAccessed
        String sortDirection     // asc, desc
) {
    public LinkSearchRequest {
        // Default values
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "dateCreated";
        }
        if (sortDirection == null || sortDirection.isEmpty()) {
            sortDirection = "desc";
        }
    }
}