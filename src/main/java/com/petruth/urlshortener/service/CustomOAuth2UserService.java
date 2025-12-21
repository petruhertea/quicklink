package com.petruth.urlshortener.service;

import com.petruth.urlshortener.entity.User;
import com.petruth.urlshortener.entity.UserOAuthProvider;
import com.petruth.urlshortener.repository.UserOAuthProviderRepository;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;
    private final UserOAuthProviderRepository oauthProviderRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public CustomOAuth2UserService(UserService userService,
                                   UserOAuthProviderRepository oauthProviderRepository) {
        this.userService = userService;
        this.oauthProviderRepository = oauthProviderRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();

        // Extract user information based on provider
        String oauthId;
        String email;
        String name;
        String picture;

        if ("github".equals(provider)) {
            Object idObj = oauth2User.getAttribute("id");
            oauthId = (idObj != null) ? idObj.toString() : null;

            email = oauth2User.getAttribute("email");

            if (email == null || email.trim().isEmpty()) {
                email = fetchGitHubEmail(userRequest);
            }

            name = oauth2User.getAttribute("name");

            if (name == null || name.trim().isEmpty()) {
                name = oauth2User.getAttribute("login");
            }

            picture = oauth2User.getAttribute("avatar_url");

        } else if ("google".equals(provider)) {
            oauthId = oauth2User.getAttribute("sub");
            email = oauth2User.getAttribute("email");
            name = oauth2User.getAttribute("name");
            picture = oauth2User.getAttribute("picture");

        } else {
            Object idAttribute = oauth2User.getAttribute("sub");
            if (idAttribute == null) {
                idAttribute = oauth2User.getAttribute("id");
            }
            oauthId = (idAttribute != null) ? idAttribute.toString() : null;

            email = oauth2User.getAttribute("email");
            name = oauth2User.getAttribute("name");
            picture = oauth2User.getAttribute("picture");
        }

        // Validation
        if (oauthId == null || oauthId.trim().isEmpty()) {
            throw new OAuth2AuthenticationException("OAuth ID is missing");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new OAuth2AuthenticationException("Email is required but not provided by " + provider);
        }

        if (name == null || name.trim().isEmpty()) {
            name = "User";
        }

        // Check if this OAuth provider connection already exists
        UserOAuthProvider oauthProvider = oauthProviderRepository
                .findByOauthProviderAndOauthId(provider, oauthId)
                .orElse(null);

        User user;

        if (oauthProvider != null) {
            // Existing OAuth connection - get the user
            user = oauthProvider.getUser();

            // Update last used timestamp
            oauthProvider.setLastUsed(LocalDateTime.now());
            oauthProvider.setProfilePicture(picture);
            oauthProviderRepository.save(oauthProvider);

        } else {
            // New OAuth connection - check if user exists by email
            user = userService.findByEmail(email).orElse(null);

            if (user == null) {
                // Create new user
                user = new User();
                user.setEmail(email);
                user.setName(name);
                user.setProfilePicture(picture);
                user = userService.save(user);
            }

            // Link this OAuth provider to the user
            oauthProvider = new UserOAuthProvider(user, provider, oauthId);
            oauthProvider.setProfilePicture(picture);
            oauthProvider.setLastUsed(LocalDateTime.now());
            oauthProviderRepository.save(oauthProvider);
        }

        // Update user info if changed (use most recent info)
        boolean needsUpdate = false;

        if (!email.equals(user.getEmail())) {
            user.setEmail(email);
            needsUpdate = true;
        }

        if (!name.equals(user.getName())) {
            user.setName(name);
            needsUpdate = true;
        }

        if (picture != null && !picture.equals(user.getProfilePicture())) {
            user.setProfilePicture(picture);
            needsUpdate = true;
        }

        if (needsUpdate) {
            userService.save(user);
        }

        return oauth2User;
    }

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
                for (Map<String, Object> emailData : emails) {
                    Boolean primary = (Boolean) emailData.get("primary");
                    Boolean verified = (Boolean) emailData.get("verified");

                    if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                        return (String) emailData.get("email");
                    }
                }

                for (Map<String, Object> emailData : emails) {
                    Boolean verified = (Boolean) emailData.get("verified");
                    if (Boolean.TRUE.equals(verified)) {
                        return (String) emailData.get("email");
                    }
                }

                return (String) emails.get(0).get("email");
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch GitHub email: " + e.getMessage());
        }

        return null;
    }
}