package com.petruth.urlshortener.service;

import com.petruth.urlshortener.entity.User;
import com.petruth.urlshortener.entity.UserOAuthProvider;

import java.util.Optional;

public interface UserService {
    Optional<User> findByEmail(String email);
    Optional<UserOAuthProvider> findByOauthProviderAndOauthId(String provider, String oauthId);
    User save(User user);
    Optional<User> findById(Long id);
}
