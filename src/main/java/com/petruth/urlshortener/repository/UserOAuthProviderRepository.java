package com.petruth.urlshortener.repository;

import com.petruth.urlshortener.entity.UserOAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserOAuthProviderRepository extends JpaRepository<UserOAuthProvider, Long> {
    Optional<UserOAuthProvider> findByOauthProviderAndOauthId(String oauthProvider, String oauthId);
}
