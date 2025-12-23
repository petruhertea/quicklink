package com.petruth.urlshortener.service;

import com.petruth.urlshortener.entity.User;
import com.petruth.urlshortener.entity.UserOAuthProvider;
import com.petruth.urlshortener.repository.UserOAuthProviderRepository;
import com.petruth.urlshortener.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserOAuthProviderRepository userOAuthProviderRepository;

    public UserServiceImpl(UserRepository userRepository,UserOAuthProviderRepository userOAuthProviderRepository) {
        this.userRepository = userRepository;
        this.userOAuthProviderRepository = userOAuthProviderRepository;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<UserOAuthProvider> findByOauthProviderAndOauthId(String provider, String oauthId) {
        return userOAuthProviderRepository.findByOauthProviderAndOauthId(provider, oauthId);
    }

    public Optional<UserOAuthProvider> findByOAuth(String provider, String oauthId) {
        return userOAuthProviderRepository.findByOauthProviderAndOauthId(provider,oauthId);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}
