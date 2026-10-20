package com.mealplanner.user;

import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Donne accès à l'utilisateur connecté. Le compte applicatif est créé automatiquement
 * à sa première requête (provisioning "just-in-time") à partir des claims du jeton.
 */
@Service
public class CurrentUserService {

    private final AppUserRepository repository;

    public CurrentUserService(AppUserRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AppUser currentUser() {
        Jwt jwt = currentJwt();
        String externalId = jwt.getSubject();
        AppUser user = repository.findByExternalId(externalId).orElse(null);
        if (user == null) {
            repository.insertIfAbsent(externalId, username(jwt));
            user = repository.findByExternalId(externalId).orElseThrow();
        }
        syncProfile(user, jwt);
        return user;
    }

    /** Identifiant de l'utilisateur connecté (raccourci le plus courant). */
    @Transactional
    public Long currentUserId() {
        return currentUser().getId();
    }

    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private void syncProfile(AppUser user, Jwt jwt) {
        String username = username(jwt);
        String email = jwt.getClaimAsString("email");
        String displayName = jwt.getClaimAsString("name");
        if (!Objects.equals(user.getUsername(), username)) {
            user.setUsername(username);
        }
        if (!Objects.equals(user.getEmail(), email)) {
            user.setEmail(email);
        }
        if (!Objects.equals(user.getDisplayName(), displayName)) {
            user.setDisplayName(displayName);
        }
    }

    private static String username(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return username != null ? username : jwt.getSubject();
    }

    private static Jwt currentJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("Aucun utilisateur authentifié");
        }
        return jwt;
    }
}
