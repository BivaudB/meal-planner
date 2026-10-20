package com.mealplanner.user;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Utilisateur applicatif. Les identifiants et mots de passe sont gérés par Keycloak ;
 * on ne conserve ici que le lien ({@code externalId} = claim "sub") et les infos de profil.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String externalId;

    @Column(nullable = false)
    private String username;

    private String email;

    private String displayName;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected AppUser() {
    }

    public AppUser(String externalId, String username) {
        this.externalId = externalId;
        this.username = username;
    }

    public Long getId() { return id; }
    public String getExternalId() { return externalId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Instant getCreatedAt() { return createdAt; }
}
