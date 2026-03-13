package com.petruth.urlshortener.service;

import com.petruth.urlshortener.entity.User;
import com.petruth.urlshortener.entity.UserOAuthProvider;
import com.petruth.urlshortener.repository.UserOAuthProviderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CustomOidcUserService extends OidcUserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOidcUserService.class);

    private final UserService userService;
    private final UserOAuthProviderRepository oauthProviderRepository;
    private final WelcomeEmailService welcomeEmailService;

    public CustomOidcUserService(UserService userService,
                                 UserOAuthProviderRepository oauthProviderRepository,
                                 WelcomeEmailService welcomeEmailService) {
        this.userService             = userService;
        this.oauthProviderRepository = oauthProviderRepository;
        this.welcomeEmailService     = welcomeEmailService;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("=== Starting OAuth login process ===");

        OidcUser oidcUser = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();
        log.info("Provider: {}", provider);

        // Extract user information based on provider
        String oauthId;
        String email;
        String name;
        String picture;

        if ("microsoft".equals(provider)) {
            log.info("Processing Microsoft login");
            // Microsoft uses 'oid' (object identifier) or 'sub' for unique ID
            oauthId = oidcUser.getAttribute("oid");
            if (oauthId == null) {
                oauthId = oidcUser.getAttribute("sub");
            }

            // Microsoft email
            email = oidcUser.getAttribute("email");
            if (email == null || email.trim().isEmpty()) {
                email = oidcUser.getAttribute("preferred_username");
            }

            // Microsoft name
            name = oidcUser.getAttribute("name");
            if (name == null || name.trim().isEmpty()) {
                name = oidcUser.getAttribute("given_name");
            }

            picture = oidcUser.getAttribute("picture");

        } else {
            log.warn("Unknown provider: {}, using fallback extraction", provider);
            // Fallback for unknown providers
            Object idAttribute = oidcUser.getAttribute("sub");
            if (idAttribute == null) {
                idAttribute = oidcUser.getAttribute("id");
            }
            oauthId = (idAttribute != null) ? idAttribute.toString() : null;

            email = oidcUser.getAttribute("email");
            name = oidcUser.getAttribute("name");
            picture = oidcUser.getAttribute("picture");
        }

        log.info("Extracted OAuth ID: {}", oauthId);
        log.info("Extracted Email: {}", email);
        log.info("Extracted Name: {}", name);
        log.info("Extracted Picture: {}", picture != null ? "Present" : "Null");

        // Validation
        if (oauthId == null || oauthId.trim().isEmpty()) {
            log.error("OAuth ID is missing for provider: {}", provider);
            throw new OAuth2AuthenticationException("OAuth ID is missing for provider: " + provider);
        }

        if (email == null || email.trim().isEmpty()) {
            log.error("Email is missing for provider: {}", provider);
            throw new OAuth2AuthenticationException("Email is required but not provided by " + provider);
        }

        if (name == null || name.trim().isEmpty()) {
            name = email.split("@")[0]; // Use email prefix as fallback
            log.info("Using email prefix as name: {}", name);
        }

        // Check if this OAuth provider connection already exists
        log.info("Checking for existing OAuth provider: {} with ID: {}", provider, oauthId);
        UserOAuthProvider oauthProvider = oauthProviderRepository
                .findByOauthProviderAndOauthId(provider, oauthId)
                .orElse(null);

        User user;

        if (oauthProvider != null) {
            log.info("Found existing OAuth provider, user ID: {}", oauthProvider.getUser().getId());
            // Existing OAuth connection - get the user
            user = oauthProvider.getUser();

            // Update last used timestamp and profile picture
            oauthProvider.setLastUsed(LocalDateTime.now());
            if (picture != null && !picture.trim().isEmpty()) {
                oauthProvider.setProfilePicture(picture);
            }
            oauthProvider = oauthProviderRepository.save(oauthProvider);
            log.info("Updated OAuth provider last used time");

            // Update user's profile picture to the most recent one
            if (picture != null && !picture.trim().isEmpty() && !picture.equals(user.getProfilePicture())) {
                user.setProfilePicture(picture);
                user = userService.save(user);
                log.info("Updated user profile picture");
            }

        } else {
            log.info("No existing OAuth provider found, checking for user by email: {}", email);
            // New OAuth connection - check if user exists by email
            user = userService.findByEmail(email).orElse(null);

            if (user == null) {
                log.info("No existing user found, creating new user");
                // Create new user
                user = new User();
                user.setEmail(email);
                user.setName(name);
                user.setProfilePicture(picture);
                user = userService.save(user);
                log.info("Created new user with ID: {}", user.getId());

                welcomeEmailService.sendWelcomeEmail(user);
            } else {
                log.info("Found existing user by email, ID: {}, linking new OAuth provider", user.getId());

                // Update user's profile picture if the new one is available
                if (picture != null && !picture.trim().isEmpty() && !picture.equals(user.getProfilePicture())) {
                    user.setProfilePicture(picture);
                    user = userService.save(user);
                    log.info("Updated existing user's profile picture");
                }
            }

            // Link this OAuth provider to the user
            log.info("Creating new OAuth provider link: {} -> User ID: {}", provider, user.getId());
            oauthProvider = new UserOAuthProvider(user, provider, oauthId);
            if (picture != null && !picture.trim().isEmpty()) {
                oauthProvider.setProfilePicture(picture);
            }
            oauthProvider.setLastUsed(LocalDateTime.now());

            try {
                oauthProvider = oauthProviderRepository.saveAndFlush(oauthProvider);
                log.info("Successfully saved OAuth provider with ID: {}", oauthProvider.getId());
            } catch (Exception e) {
                log.error("Failed to save OAuth provider", e);
                throw new OAuth2AuthenticationException("Failed to save OAuth provider: " + e.getMessage());
            }
        }

        // Update user info if changed (use most recent info)
        boolean needsUpdate = false;

        if (!email.equals(user.getEmail())) {
            log.info("Email changed from {} to {}", user.getEmail(), email);
            user.setEmail(email);
            needsUpdate = true;
        }

        if (!name.equals(user.getName())) {
            log.info("Name changed from {} to {}", user.getName(), name);
            user.setName(name);
            needsUpdate = true;
        }

        // Always update profile picture to the latest one from any provider
        if (picture != null && !picture.equals(user.getProfilePicture())) {
            log.info("Profile picture updated");
            user.setProfilePicture(picture);
            needsUpdate = true;
        }

        if (needsUpdate) {
            user = userService.save(user);
            log.info("Saved user updates");
        }

        log.info("=== OAuth login completed successfully ===");
        log.info("Final state - User ID: {}, Email: {}, Provider: {}, OAuth ID: {}",
                user.getId(), user.getEmail(), provider, oauthId);

        return oidcUser;
    }
}