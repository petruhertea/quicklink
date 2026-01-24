package com.petruth.urlshortener.config;


import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class HealthCheckConfig {

    /**
     * Custom health indicator for database connectivity
     * Cloud Run uses this to determine if instance is healthy
     */
    @Bean
    public HealthIndicator databaseHealthIndicator(DataSource dataSource) {
        return () -> {
            try {
                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                return Health.up()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("status", "Connected")
                        .build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }

    /**
     * Cache health indicator
     */
    @Bean
    public HealthIndicator cacheHealthIndicator() {
        return () -> {
            try {
                // Simple check - Caffeine cache is always available
                return Health.up()
                        .withDetail("cache", "Caffeine")
                        .withDetail("status", "Active")
                        .build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("cache", "Caffeine")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }
}