package com.mealplanner.food;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.mealplanner.food.FoodDtos.FoodDetail;
import com.mealplanner.food.FoodDtos.FoodSummary;
import com.mealplanner.food.FoodDtos.NutrientValue;

/**
 * Accès en lecture au référentiel Ciqual. En SQL direct (JdbcClient) car les requêtes
 * s'appuient sur la recherche approximative de PostgreSQL (pg_trgm).
 */
@Repository
public class FoodRepository {

    /**
     * Recherche approximative, insensible à la casse et aux accents.
     * word_similarity mesure si le texte recherché apparaît (même partiellement) dans le nom.
     * Départage : noms qui commencent par la recherche, puis noms les plus courts
     * ("Tomate, crue" passe avant "Sauce tomate aux oignons, préemballée").
     */
    private static final String SEARCH_SQL = """
        SELECT f.id, f.name, g.name AS group_name, f.energy_kcal, f.protein_g, f.carbohydrate_g, f.fat_g,
               word_similarity(f_normalize(:q), f_normalize(f.name)) AS score
        FROM food f
        LEFT JOIN food_group g ON g.code = f.group_code
        WHERE f_normalize(f.name) %> f_normalize(:q)
           OR f_normalize(f.name) LIKE '%' || f_normalize(:q) || '%'
        ORDER BY score DESC,
                 (f_normalize(f.name) LIKE f_normalize(:q) || '%') DESC,
                 length(f.name) ASC
        LIMIT :limit
        """;

    private final JdbcClient jdbc;

    public FoodRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<FoodSummary> search(String query, int limit) {
        return jdbc.sql(SEARCH_SQL)
            .param("q", query)
            .param("limit", limit)
            .query((rs, i) -> new FoodSummary(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("group_name"),
                rs.getBigDecimal("energy_kcal"),
                rs.getBigDecimal("protein_g"),
                rs.getBigDecimal("carbohydrate_g"),
                rs.getBigDecimal("fat_g"),
                rs.getDouble("score")))
            .list();
    }

    /** Meilleur aliment correspondant à un nom d'ingrédient, si son score atteint le seuil. */
    public Optional<FoodSummary> bestMatch(String ingredientName, double threshold) {
        return search(ingredientName, 1).stream()
            .filter(food -> food.score() != null && food.score() >= threshold)
            .findFirst();
    }

    public Optional<FoodDetail> findDetail(long id) {
        List<NutrientValue> nutrients = jdbc.sql("""
                SELECT n.id, n.name, n.unit, fn.value, fn.qualifier, fn.raw_value
                FROM food_nutrient fn JOIN nutrient n ON n.id = fn.nutrient_id
                WHERE fn.food_id = :id
                ORDER BY n.sort_order
                """)
            .param("id", id)
            .query((rs, i) -> new NutrientValue(
                rs.getInt("id"), rs.getString("name"), rs.getString("unit"),
                rs.getBigDecimal("value"), rs.getString("qualifier"), rs.getString("raw_value")))
            .list();

        return jdbc.sql("""
                SELECT f.*, g.name AS group_name
                FROM food f LEFT JOIN food_group g ON g.code = f.group_code
                WHERE f.id = :id
                """)
            .param("id", id)
            .query((rs, i) -> new FoodDetail(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("scientific_name"),
                rs.getString("group_name"),
                rs.getBigDecimal("energy_kcal"),
                rs.getBigDecimal("energy_kj"),
                rs.getBigDecimal("protein_g"),
                rs.getBigDecimal("carbohydrate_g"),
                rs.getBigDecimal("sugars_g"),
                rs.getBigDecimal("fat_g"),
                rs.getBigDecimal("saturated_fat_g"),
                rs.getBigDecimal("fiber_g"),
                rs.getBigDecimal("salt_g"),
                nutrients))
            .optional();
    }

    /** Valeurs clés (pour 100 g) d'un ensemble d'aliments, indexées par identifiant. */
    public Map<Long, FoodSummary> findSummaries(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap(); // (contrairement à Map.of(), get(null) renvoie null)
        }
        return jdbc.sql("""
                SELECT f.id, f.name, g.name AS group_name, f.energy_kcal, f.protein_g, f.carbohydrate_g, f.fat_g
                FROM food f LEFT JOIN food_group g ON g.code = f.group_code
                WHERE f.id IN (:ids)
                """)
            .param("ids", ids)
            .query((rs, i) -> new FoodSummary(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("group_name"),
                rs.getBigDecimal("energy_kcal"),
                rs.getBigDecimal("protein_g"),
                rs.getBigDecimal("carbohydrate_g"),
                rs.getBigDecimal("fat_g"),
                null))
            .list().stream()
            .collect(Collectors.toMap(FoodSummary::id, Function.identity()));
    }

    public boolean exists(long id) {
        return jdbc.sql("SELECT count(*) FROM food WHERE id = :id").param("id", id).query(Long.class).single() > 0;
    }

    public long count() {
        return jdbc.sql("SELECT count(*) FROM food").query(Long.class).single();
    }
}
