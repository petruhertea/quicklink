package com.petruth.urlshortener.config;

import com.petruth.urlshortener.service.CustomOAuth2UserService;
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
    private final CustomOAuth2UserService customOAuth2UserService;

    // Constructor injection for both services
    public SecurityConfig(CustomOidcUserService customOidcUserService,
                          CustomOAuth2UserService customOAuth2UserService) {
        this.customOidcUserService = customOidcUserService;
        this.customOAuth2UserService = customOAuth2UserService;
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
                                // For OIDC providers (Microsoft, etc.)
                                .oidcUserService(customOidcUserService)
                                // For OAuth2 providers (Google, GitHub, etc.)
                                .userService(customOAuth2UserService)
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
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .xssProtection(xss -> xss
                                .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                        )
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                        "script-src 'self' https://pagead2.googlesyndication.com/pagead/js/"+
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