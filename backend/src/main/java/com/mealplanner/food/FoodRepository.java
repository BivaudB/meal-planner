package com.mealplanner.food;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Aliments du référentiel Ciqual. Le catalogue (filtres facultatifs, tri au choix) passe par
 * {@link JpaSpecificationExecutor}, voir {@link FoodService#catalog}.
 */
public interface FoodRepository extends JpaRepository<Food, Long>, JpaSpecificationExecutor<Food> {

    /**
     * Recherche approximative, insensible à la casse et aux accents (pg_trgm, d'où le SQL natif).
     * word_similarity mesure si le texte recherché apparaît (même partiellement) dans le nom.
     * Départage : noms qui commencent par la recherche, puis noms les plus courts
     * ("Tomate" passe avant "Tomate cerise").
     */
    @Query(value = """
        SELECT f.id, f.name, g.name AS "groupName", f.energy_kcal AS "energyKcal", f.protein_g AS "proteinG",
               f.carbohydrate_g AS "carbohydrateG", f.fat_g AS "fatG",
               word_similarity(f_normalize(:q), f_normalize(f.name)) AS score
        FROM food f
        LEFT JOIN food_group g ON g.code = f.group_code
        WHERE f_normalize(f.name) %> f_normalize(:q)
           OR f_normalize(f.name) LIKE '%' || f_normalize(:q) || '%'
        ORDER BY score DESC,
                 (f_normalize(f.name) LIKE f_normalize(:q) || '%') DESC,
                 length(f.name) ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<ScoredFood> search(@Param("q") String query, @Param("limit") int limit);

    /** Page du catalogue, groupe Ciqual chargé dans la même requête. */
    @Override
    @EntityGraph(attributePaths = "group")
    Page<Food> findAll(Specification<Food> spec, Pageable pageable);

    @Query("SELECT f FROM Food f LEFT JOIN FETCH f.group WHERE f.id = :id")
    Optional<Food> findWithGroupById(@Param("id") long id);

    @Query("SELECT f FROM Food f LEFT JOIN FETCH f.group WHERE f.id IN :ids")
    List<Food> findWithGroupByIdIn(@Param("ids") Collection<Long> ids);

    /** Nombre d'aliments par groupe Ciqual (au niveau le plus fin). */
    @Query("SELECT f.group.code AS groupCode, count(f) AS foodCount FROM Food f WHERE f.group IS NOT NULL GROUP BY f.group.code")
    List<GroupCount> countByGroup();

    /** Résultat de {@link #search} : valeurs clés pour 100 g et score de similarité. */
    interface ScoredFood {
        Long getId();
        String getName();
        String getGroupName();
        BigDecimal getEnergyKcal();
        BigDecimal getProteinG();
        BigDecimal getCarbohydrateG();
        BigDecimal getFatG();
        Double getScore();
    }

    interface GroupCount {
        String getGroupCode();
        long getFoodCount();
    }
}
