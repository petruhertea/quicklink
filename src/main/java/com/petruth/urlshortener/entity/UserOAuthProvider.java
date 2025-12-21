package com.petruth.urlshortener.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_oauth_providers",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"oauth_provider", "oauth_id"})
        }
)
public class UserOAuthProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "oauth_provider", nullable = false)
    private String oauthProvider;

    @Column(name = "oauth_id", nullable = false)
    private String oauthId;

    @Column(name = "profile_picture")
    private String profilePicture;

    @CreationTimestamp
    @Column(name = "linked_at")
    private LocalDateTime linkedAt;

    @Column(name = "last_used")
    private LocalDateTime lastUsed;

    // Constructors
    public UserOAuthProvider() {}

    public UserOAuthProvider(User user, String oauthProvider, String oauthId) {
        this.user = user;
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getOauthProvider() { return oauthProvider; }
    public void setOauthProvider(String oauthProvider) { this.oauthProvider = oauthProvider; }

    public String getOauthId() { return oauthId; }
    public void setOauthId(String oauthId) { this.oauthId = oauthId; }

    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

    public LocalDateTime getLinkedAt() { return linkedAt; }
    public void setLinkedAt(LocalDateTime linkedAt) { this.linkedAt = linkedAt; }

    public LocalDateTime getLastUsed() { return lastUsed; }
    public void setLastUsed(LocalDateTime lastUsed) { this.lastUsed = lastUsed; }
}
