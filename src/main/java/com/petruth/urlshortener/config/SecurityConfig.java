package com.petruth.urlshortener.config;

import com.petruth.urlshortener.service.CustomOidcUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOidcUserService customOidcUserService;

    // Constructor injection
    public SecurityConfig(CustomOidcUserService customOidcUserService) {
        this.customOidcUserService = customOidcUserService;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/", "/api/shorten", "/api/{code}", "/login", "/css/**", "/js/**", "/error", "/payment/webhook").permitAll();
                    auth.requestMatchers("/dashboard/**", "/subscription", "/analytics/**", "/api/analytics/**", "/actuator/**", "/payment/**").authenticated();
                    auth.anyRequest().authenticated();
                })
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(customOidcUserService)
                        )
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()
                        .maximumSessions(3)
                        .maxSessionsPreventsLogin(false)
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**", "/payment/**")
                )
                .headers(headers -> headers
                        // Prevent clickjacking
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        // XSS Protection
                        .xssProtection(xss -> xss
                                .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                        )
                        // Content Security Policy
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                        "img-src 'self' https://lh3.googleusercontent.com https://avatars.githubusercontent.com data:; " +
                                        "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
                                        "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
                                        "font-src 'self' https://cdn.jsdelivr.net; " +
                                        "connect-src 'self' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com"
                        ))
                )
                .build();
    }
}