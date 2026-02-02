package com.petruth.urlshortener.config;
import com.petruth.urlshortener.interceptor.AdInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdInterceptor adInterceptor;

    public WebMvcConfig(AdInterceptor adInterceptor) {
        this.adInterceptor = adInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adInterceptor)
                // Only page-rendering routes need the ad flag
                .addPathPatterns("/", "/dashboard/**", "/analytics/**",
                        "/subscription", "/bulk-shorten", "/login",
                        "/payment/**")
                // Explicitly exclude anything that isn't a rendered page
                .excludePathPatterns("/api/**", "/actuator/**",
                        "/oauth2/**", "/login/oauth2/**",
                        "/css/**", "/js/**", "/error");
    }
}