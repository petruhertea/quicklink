package com.petruth.urlshortener.service;

import com.petruth.urlshortener.entity.User;
import com.petruth.urlshortener.entity.UserOAuthProvider;
import com.petruth.urlshortener.repository.UserOAuthProviderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Custom OAuth2 User Service for non-OIDC providers (like Google when configured as OAuth2)
 * This handles the same logic as CustomOidcUserService but for OAuth2UserRequest
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserService userService;
    private final UserOAuthProviderRepository oauthProviderRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public CustomOAuth2UserService(UserService userService,
                                   UserOAuthProviderRepository oauthProviderRepository) {
        this.userService = userService;
        this.oauthProviderRepository = oauthProviderRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("=== Starting OAuth2 login process ===");

        OAuth2User oauth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();
        log.info("Provider: {}", provider);

        // Extract user information based on provider
        String oauthId;
        String email;
        String name;
        String picture;

        if ("google".equals(provider)) {
            log.info("Processing Google OAuth2 login");
            oauthId = oauth2User.getAttribute("sub");
            email = oauth2User.getAttribute("email");
            name = oauth2User.getAttribute("name");
            picture = oauth2User.getAttribute("picture");

        } else if ("github".equals(provider)) {
            log.info("Processing GitHub OAuth2 login");
            Object idObj = oauth2User.getAttribute("id");
            oauthId = idObj != null ? idObj.toString() : null;

            // GitHub email might be null if not public
            email = oauth2User.getAttribute("email");
            if (email == null || email.trim().isEmpty()) {
                log.info("Email not in GitHub user attributes, fetching from API");
                email = fetchGitHubEmail(userRequest);
            }

            name = oauth2User.getAttribute("name");
            if (name == null || name.trim().isEmpty()) {
                name = oauth2User.getAttribute("login");
            }
            picture = oauth2User.getAttribute("avatar_url");

        } else {
            log.warn("Unknown OAuth2 provider: {}, using fallback extraction", provider);
            Object idAttribute = oauth2User.getAttribute("sub");
            if (idAttribute == null) {
                idAttribute = oauth2User.getAttribute("id");
            }
            oauthId = (idAttribute != null) ? idAttribute.toString() : null;
            email = oauth2User.getAttribute("email");
            name = oauth2User.getAttribute("name");
            picture = oauth2User.getAttribute("picture");
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
            name = email.split("@")[0];
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
            user = oauthProvider.getUser();

            // Update last used timestamp and profile picture
            oauthProvider.setLastUsed(LocalDateTime.now());
            if (picture != null && !picture.trim().isEmpty()) {
                oauthProvider.setProfilePicture(picture);
            }
            oauthProvider = oauthProviderRepository.saveAndFlush(oauthProvider);
            log.info("Updated OAuth provider last used time");

            // Update user's profile picture to the most recent one
            if (picture != null && !picture.trim().isEmpty() && !picture.equals(user.getProfilePicture())) {
                user.setProfilePicture(picture);
                user = userService.save(user);
                log.info("Updated user profile picture");
            }

        } else {
            log.info("No existing OAuth provider found, checking for user by email: {}", email);
            user = userService.findByEmail(email).orElse(null);

            if (user == null) {
                log.info("No existing user found, creating new user");
                user = new User();
                user.setEmail(email);
                user.setName(name);
                user.setProfilePicture(picture);
                user = userService.save(user);
                log.info("Created new user with ID: {}", user.getId());
            } else {
                log.info("Found existing user by email, ID: {}, linking new OAuth provider", user.getId());

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

        // Update user info if changed
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

        if (picture != null && !picture.equals(user.getProfilePicture())) {
            log.info("Profile picture updated");
            user.setProfilePicture(picture);
            needsUpdate = true;
        }

        if (needsUpdate) {
            user = userService.save(user);
            log.info("Saved user updates");
        }

        log.info("=== OAuth2 login completed successfully ===");
        log.info("Final state - User ID: {}, Email: {}, Provider: {}, OAuth ID: {}",
                user.getId(), user.getEmail(), provider, oauthId);

        return oauth2User;
    }

    /**
     * Fetch GitHub email from API (for OAuth2UserRequest)
     * GitHub doesn't always include email in the user attributes
     */
    private String fetchGitHubEmail(OAuth2UserRequest userRequest) {
        try {
            String accessToken = userRequest.getAccessToken().getTokenValue();

            RequestEntity<Void> request = RequestEntity
                    .get(URI.create("https://api.github.com/user/emails"))
                    .header("Authorization", "Bearer " + accessToken)
                    .build();

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    request,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> emails = response.getBody();

            if (emails != null && !emails.isEmpty()) {
                // Try to find primary verified email
                for (Map<String, Object> emailData : emails) {
                    Boolean primary = (Boolean) emailData.get("primary");
                    Boolean verified = (Boolean) emailData.get("verified");

                    if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                        String email = (String) emailData.get("email");
                        log.info("Found primary verified GitHub email: {}", email);
                        return email;
                    }
                }

                // Fallback to any verified email
                for (Map<String, Object> emailData : emails) {
                    Boolean verified = (Boolean) emailData.get("verified");
                    if (Boolean.TRUE.equals(verified)) {
                        String email = (String) emailData.get("email");
                        log.info("Found verified GitHub email: {}", email);
                        return email;
                    }
                }

                // Last resort: use first email
                String email = (String) emails.get(0).get("email");
                log.warn("Using first GitHub email (not verified): {}", email);
                return email;
            }
        } catch (Exception e) {
            log.error("Failed to fetch GitHub email: {}", e.getMessage(), e);
        }

        return null;
    }
}