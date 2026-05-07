package com.petruth.urlshortener.config;


import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
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
                // Test actual connectivity
                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                jdbcTemplate.queryForObject("SELECT 1", Integer.class);

                // Also check HikariCP pool state
                if (dataSource instanceof HikariDataSource hikari) {
                    HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
                    int waiting = pool.getThreadsAwaitingConnection();
                    if (waiting > 3) {
                        return Health.down()
                                .withDetail("reason", "Connection pool near exhaustion")
                                .withDetail("threads_waiting", waiting)
                                .build();
                    }
                    return Health.up()
                            .withDetail("active_connections", pool.getActiveConnections())
                            .withDetail("idle_connections", pool.getIdleConnections())
                            .withDetail("threads_waiting", waiting)
                            .build();
                }

                return Health.up().withDetail("database", "Connected").build();
            } catch (Exception e) {
                return Health.down()
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