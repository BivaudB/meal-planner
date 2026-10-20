package com.mealplanner.recipe;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    /** {@code query} vide = pas de filtre sur le nom. */
    @Query("""
        SELECT r FROM Recipe r
        WHERE r.ownerId = :ownerId
          AND LOWER(r.name) LIKE LOWER(CONCAT('%', :q, '%'))
        """)
    Page<Recipe> findOwned(@Param("ownerId") Long ownerId, @Param("q") String query, Pageable pageable);

    @Query("""
        SELECT r FROM Recipe r
        WHERE r.visibility = :visibility
          AND LOWER(r.name) LIKE LOWER(CONCAT('%', :q, '%'))
        """)
    Page<Recipe> findByVisibility(@Param("visibility") Recipe.Visibility visibility, @Param("q") String query,
                                  Pageable pageable);
}
