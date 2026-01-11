package com.petruth.urlshortener.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shortened_url", indexes = {
        @Index(name = "idx_code", columnList = "code")
})
public class ShortenedUrl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FIXED: Explicitly define column type to prevent bytea issue
    @Column(name = "long_url", length = 2048, columnDefinition = "VARCHAR(2048)")
    private String longUrl;

    @Column(name = "short_url", unique = true, columnDefinition = "VARCHAR(255)")
    private String shortUrl;

    @Column(name = "code", unique = true, columnDefinition = "VARCHAR(255)")
    private String code;

    @CreationTimestamp
    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "click_count")
    private Long clickCount = 0L;

    @Column(name = "last_accessed")
    private LocalDateTime lastAccessed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(
            mappedBy = "shortenedUrl",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true
    )
    private List<ClickAnalytics> analytics = new ArrayList<>();

    public ShortenedUrl(){}

    public ShortenedUrl(Long id, String longUrl, String shortUrl, String code, LocalDateTime dateCreated, LocalDateTime expiresAt, Long clickCount, LocalDateTime lastAccessed, User user, List<ClickAnalytics> analytics) {
        this.id = id;
        this.longUrl = longUrl;
        this.shortUrl = shortUrl;
        this.code = code;
        this.dateCreated = dateCreated;
        this.expiresAt = expiresAt;
        this.clickCount = clickCount;
        this.lastAccessed = lastAccessed;
        this.user = user;
        this.analytics = analytics;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Long getClickCount() {
        return clickCount;
    }

    public void setClickCount(Long clickCount) {
        this.clickCount = clickCount;
    }

    public LocalDateTime getLastAccessed() {
        return lastAccessed;
    }

    public void setLastAccessed(LocalDateTime lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<ClickAnalytics> getAnalytics() {
        return analytics;
    }

    public void setAnalytics(List<ClickAnalytics> analytics) {
        this.analytics = analytics;
    }
}