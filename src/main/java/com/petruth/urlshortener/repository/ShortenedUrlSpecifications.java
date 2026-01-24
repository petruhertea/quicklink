package com.petruth.urlshortener.repository;

import com.petruth.urlshortener.entity.ShortenedUrl;
import com.petruth.urlshortener.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ShortenedUrlSpecifications {

    /**
     * Base specification - user must match
     */
    public static Specification<ShortenedUrl> belongsToUser(User user) {
        return (root, query, cb) -> cb.equal(root.get("user"), user);
    }

    /**
     * Search in code OR long_url (case-insensitive)
     */
    public static Specification<ShortenedUrl> searchByTerm(String searchTerm) {
        return (root, query, cb) -> {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return cb.conjunction(); // Always true
            }

            String likePattern = "%" + searchTerm.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("code")), likePattern),
                    cb.like(cb.lower(root.get("longUrl")), likePattern)
            );
        };
    }

    /**
     * Filter by date range
     */
    public static Specification<ShortenedUrl> createdBetween(
            LocalDateTime startDate,
            LocalDateTime endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateCreated"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dateCreated"), endDate));
            }

            return predicates.isEmpty() ?
                    cb.conjunction() :
                    cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Filter by click count range
     */
    public static Specification<ShortenedUrl> clicksBetween(Long minClicks, Long maxClicks) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (minClicks != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("clickCount"), minClicks));
            }
            if (maxClicks != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("clickCount"), maxClicks));
            }

            return predicates.isEmpty() ?
                    cb.conjunction() :
                    cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Filter by expiration status
     */
    public static Specification<ShortenedUrl> isExpired(Boolean expired) {
        return (root, query, cb) -> {
            if (expired == null) {
                return cb.conjunction(); // No filter
            }

            LocalDateTime now = LocalDateTime.now();

            if (expired) {
                // Expired = expiresAt < now
                return cb.and(
                        cb.isNotNull(root.get("expiresAt")),
                        cb.lessThan(root.get("expiresAt"), now)
                );
            } else {
                // Active = expiresAt IS NULL OR expiresAt >= now
                return cb.or(
                        cb.isNull(root.get("expiresAt")),
                        cb.greaterThanOrEqualTo(root.get("expiresAt"), now)
                );
            }
        };
    }

    /**
     * Only expired links
     */
    public static Specification<ShortenedUrl> expiredLinks() {
        return (root, query, cb) -> cb.and(
                cb.isNotNull(root.get("expiresAt")),
                cb.lessThan(root.get("expiresAt"), LocalDateTime.now())
        );
    }

    /**
     * Only active links (not expired)
     */
    public static Specification<ShortenedUrl> activeLinks() {
        return (root, query, cb) -> {
            LocalDateTime now = LocalDateTime.now();
            return cb.or(
                    cb.isNull(root.get("expiresAt")),
                    cb.greaterThanOrEqualTo(root.get("expiresAt"), now)
            );
        };
    }

    /**
     * Combined advanced search
     */
    public static Specification<ShortenedUrl> advancedSearch(
            User user,
            String searchTerm,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long minClicks,
            Long maxClicks,
            Boolean expired) {

        return Specification
                .where(belongsToUser(user))
                .and(searchByTerm(searchTerm))
                .and(createdBetween(startDate, endDate))
                .and(clicksBetween(minClicks, maxClicks))
                .and(isExpired(expired));
    }
}