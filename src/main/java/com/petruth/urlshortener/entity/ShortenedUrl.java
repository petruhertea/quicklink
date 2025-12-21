package com.petruth.urlshortener.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "shortened_url")
public class ShortenedUrl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "long_url", length = 2048)
    private String longUrl;
    @Column(name = "short_url", unique = true)
    private String shortUrl;
    @Column(name = "code", unique = true)
    private String code;
    @CreationTimestamp
    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    public ShortenedUrl(){}

    public ShortenedUrl(Long id, String longUrl, String shortUrl, String code, LocalDateTime dateCreated) {
        this.id = id;
        this.longUrl = longUrl;
        this.shortUrl = shortUrl;
        this.code = code;
        this.dateCreated = dateCreated;
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

    @Override
    public String toString() {
        return "ShortenedUrl{" +
                "id=" + id +
                ", longUrl='" + longUrl + '\'' +
                ", shortUrl='" + shortUrl + '\'' +
                ", code='" + code + '\'' +
                ", dateCreated='" + dateCreated + '\'' +
                '}';
    }
}
