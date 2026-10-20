package com.mealplanner.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByExternalId(String externalId);

    /** Création idempotente : sans effet si l'utilisateur existe déjà (requêtes simultanées). */
    @Modifying
    @Query(value = """
        INSERT INTO app_user (external_id, username) VALUES (:externalId, :username)
        ON CONFLICT (external_id) DO NOTHING
        """, nativeQuery = true)
    void insertIfAbsent(@Param("externalId") String externalId, @Param("username") String username);
}
