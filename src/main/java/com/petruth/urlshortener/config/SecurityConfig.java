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

    public SecurityConfig(CustomOidcUserService customOidcUserService,
                          CustomOAuth2UserService customOAuth2UserService) {
        this.customOidcUserService = customOidcUserService;
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/", "/api/shorten", "/api/{code}", "/login", "/css/**", "/js/**", "/error", "/payment/webhook","/QuickLink_Privacy_Policy.docx", "/QuickLink_Terms_of_Service.docx","/ads.txt").permitAll();
                    auth.requestMatchers("/dashboard/**", "/subscription", "/analytics/**", "/api/analytics/**", "/actuator/**", "/payment/**").authenticated();
                    auth.anyRequest().authenticated();
                })
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(customOidcUserService)
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

                                        // script-src – fallback for browsers that ignore the -elem/-attr variants
                                        "script-src 'self' 'unsafe-inline' " +
                                        "https://pagead2.googlesyndication.com " +
                                        "https://tpc.googlesyndication.com " +
                                        "https://www.googletagservices.com " +
                                        "https://www.google.com " +
                                        "https://cdn.jsdelivr.net " +
                                        "https://cdnjs.cloudflare.com; " +

                                        // script-src-elem – controls <script> elements specifically.
                                        // When present it COMPLETELY overrides script-src for <script> tags,
                                        // so every origin must be listed again.
                                        // 'unsafe-inline' is mandatory here: AdSense requires the inline
                                        //   <script>(adsbygoogle = window.adsbygoogle || []).push({});</script>
                                        // without it the ad never initialises and Chrome logs the exact
                                        // "Executing inline script violates …" error you saw.
                                        "script-src-elem 'self' 'unsafe-inline' " +
                                        "https://pagead2.googlesyndication.com " +
                                        "https://tpc.googlesyndication.com " +
                                        "https://www.googletagservices.com " +
                                        "https://www.google.com " +
                                        "https://cdn.jsdelivr.net " +
                                        "https://cdnjs.cloudflare.com " +
                                        "https://ep1.adtrafficquality.google " +
                                        "https://ep2.adtrafficquality.google; " +

                                        // script-src-attr – on* event-handler attributes
                                        "script-src-attr 'self' 'unsafe-inline'; " +

                                        // img-src – allow profile avatars from OAuth providers, inline data URIs,
                                        // and Google's Ad Traffic Quality sodar pixel (same ep1/ep2 subdomains
                                        // already whitelisted in frame-src and script-src-elem).
                                        "img-src 'self' https://lh3.googleusercontent.com https://avatars.githubusercontent.com " +
                                        "https://ep1.adtrafficquality.google https://ep2.adtrafficquality.google data:; " +
                                        "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
                                        "font-src 'self' https://cdn.jsdelivr.net; " +
                                        "connect-src 'self' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com " +
                                        "https://ep1.adtrafficquality.google https://ep2.adtrafficquality.google; " +

                                        // frame-src – iframes.
                                        // pagead2           – the actual ad creative rendered by Google.
                                        // ep1 / ep2         – Google's Ad Traffic Quality check runs in
                                        //                     a separate iframe from these subdomains.
                                        //                     Omitting them produces the
                                        //                     "Framing … violates … frame-src" error.
                                        "frame-src https://pagead2.googlesyndication.com " +
                                        "https://ep1.adtrafficquality.google " +
                                        "https://ep2.adtrafficquality.google"
                        ))
                )

                .build();
    }
}