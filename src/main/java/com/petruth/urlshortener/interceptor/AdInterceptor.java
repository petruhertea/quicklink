package com.petruth.urlshortener.interceptor;

import com.petruth.urlshortener.entity.User;
import com.petruth.urlshortener.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Optional;

/**
 * Adds {@code showAds = true/false} to every ModelAndView so that
 * Thymeleaf templates can conditionally render the ad banner without
 * every controller having to set the flag manually.
 *
 * <p>Logic:
 * <ul>
 *   <li>Anonymous users          → fragment shown</li>
 *   <li>Authenticated, free      → fragment shown</li>
 *   <li>Authenticated, premium   → fragment hidden</li>
 *   <li>Non-page responses (API / redirects with no MAV) → skipped entirely</li>
 * </ul>
 */
@Component
public class AdInterceptor implements HandlerInterceptor {

    private final UserService userService;

    public AdInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) {

        // Only inject into page responses that have a view (skip REST APIs)
        if (modelAndView == null || modelAndView.getView() == null && modelAndView.getViewName() == null) {
            return;
        }

        // Already set by the controller explicitly? Respect that.
        if (modelAndView.getModel().containsKey("showAds")) {
            return;
        }

        boolean showAds = true; // default: show fragment

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            // Try to find the user and check premium status
            OAuth2User principal = (auth.getPrincipal() instanceof OAuth2User)
                    ? (OAuth2User) auth.getPrincipal()
                    : null;

            if (principal != null) {
                String email = principal.getAttribute("email");
                if (email != null) {
                    Optional<User> user = userService.findByEmail(email);
                    if (user.isPresent() && user.get().isPremium()) {
                        showAds = false;
                    }
                }
            }
        }

        modelAndView.getModel().put("showAds", showAds);
    }
}