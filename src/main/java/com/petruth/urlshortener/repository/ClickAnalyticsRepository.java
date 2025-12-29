package com.petruth.urlshortener.repository;

import com.petruth.urlshortener.entity.ClickAnalytics;
import com.petruth.urlshortener.entity.ShortenedUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ClickAnalyticsRepository extends JpaRepository<ClickAnalytics, Long> {

    List<ClickAnalytics> findByShortenedUrl(ShortenedUrl shortenedUrl);

    List<ClickAnalytics> findByShortenedUrlOrderByClickedAtDesc(ShortenedUrl shortenedUrl);

    @Query("SELECT DATE(c.clickedAt) as date, COUNT(c) as count FROM ClickAnalytics c " +
            "WHERE c.shortenedUrl = :url AND c.clickedAt >= :startDate " +
            "GROUP BY DATE(c.clickedAt) ORDER BY DATE(c.clickedAt)")
    List<Object[]> getClicksByDay(@Param("url") ShortenedUrl url, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT c.country, COUNT(c) as count FROM ClickAnalytics c " +
            "WHERE c.shortenedUrl = :url AND c.country IS NOT NULL " +
            "GROUP BY c.country ORDER BY count DESC")
    List<Object[]> getClicksByCountry(@Param("url") ShortenedUrl url);

    @Query("SELECT c.deviceType, COUNT(c) as count FROM ClickAnalytics c " +
            "WHERE c.shortenedUrl = :url AND c.deviceType IS NOT NULL " +
            "GROUP BY c.deviceType")
    List<Object[]> getClicksByDevice(@Param("url") ShortenedUrl url);

    @Query("SELECT c.browser, COUNT(c) as count FROM ClickAnalytics c " +
            "WHERE c.shortenedUrl = :url AND c.browser IS NOT NULL " +
            "GROUP BY c.browser ORDER BY count DESC")
    List<Object[]> getClicksByBrowser(@Param("url") ShortenedUrl url);

    @Query("SELECT c.referer, COUNT(c) as count FROM ClickAnalytics c " +
            "WHERE c.shortenedUrl = :url AND c.referer IS NOT NULL " +
            "GROUP BY c.referer ORDER BY count DESC")
    List<Object[]> getClicksByReferer(@Param("url") ShortenedUrl url);

    long countByShortenedUrlAndClickedAtAfter(ShortenedUrl url, LocalDateTime date);
}