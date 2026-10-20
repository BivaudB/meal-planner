package com.mealplanner.recommendation;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mealplanner.recipe.RecipeDtos.Nutrition;
import com.mealplanner.recipe.RecipeService;
import com.mealplanner.user.CurrentUserService;

/**
 * Recommandations de recettes selon les ingrédients disponibles (garde-manger).
 * <p>
 * V1 : on classe les recettes (les miennes + les publiques) par part d'ingrédients
 * déjà disponibles. Un ingrédient est "disponible" s'il est rattaché au même aliment
 * Ciqual qu'un élément du garde-manger, ou s'il porte le même nom (sans accents ni casse).
 * Évolutions prévues : préférences, régime, objectifs nutritionnels.
 */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private static final String SQL = """
        WITH pantry AS (
            SELECT food_id, f_normalize(name) AS name FROM pantry_item WHERE user_id = :userId
        ),
        scored AS (
            SELECT r.id, r.name, r.servings, r.prep_minutes, r.cook_minutes, r.owner_id = :userId AS mine,
                   count(ri.id) AS total,
                   count(ri.id) FILTER (WHERE EXISTS (
                       SELECT 1 FROM pantry p
                       WHERE (p.food_id IS NOT NULL AND p.food_id = ri.food_id)
                          OR p.name = f_normalize(ri.name))) AS available,
                   array_agg(ri.name ORDER BY ri.position) FILTER (WHERE NOT EXISTS (
                       SELECT 1 FROM pantry p
                       WHERE (p.food_id IS NOT NULL AND p.food_id = ri.food_id)
                          OR p.name = f_normalize(ri.name))) AS missing
            FROM recipe r
            JOIN recipe_ingredient ri ON ri.recipe_id = r.id
            WHERE r.owner_id = :userId OR r.visibility = 'PUBLIC'
            GROUP BY r.id
        )
        SELECT * FROM scored
        WHERE available > 0 AND (total - available) <= :maxMissing
        ORDER BY available::numeric / total DESC, available DESC, name
        LIMIT :limit
        """;

    private final JdbcClient jdbc;
    private final CurrentUserService currentUser;
    private final RecipeService recipeService;

    public RecommendationController(JdbcClient jdbc, CurrentUserService currentUser, RecipeService recipeService) {
        this.jdbc = jdbc;
        this.currentUser = currentUser;
        this.recipeService = recipeService;
    }

    public record Recommendation(Long recipeId, String name, Integer servings, Integer prepMinutes,
                                 Integer cookMinutes, boolean mine, int totalIngredients,
                                 int availableIngredients, double coverage, List<String> missingIngredients,
                                 Nutrition nutrition) {
    }

    @GetMapping
    public List<Recommendation> recommend(@RequestParam(name = "maxMissing", defaultValue = "3") int maxMissing,
                                          @RequestParam(name = "limit", defaultValue = "20") int limit) {
        List<Recommendation> ranked = jdbc.sql(SQL)
            .param("userId", currentUser.currentUserId())
            .param("maxMissing", Math.clamp(maxMissing, 0, 50))
            .param("limit", Math.clamp(limit, 1, 100))
            .query((rs, i) -> {
                int total = rs.getInt("total");
                int available = rs.getInt("available");
                return new Recommendation(
                    rs.getLong("id"),
                    rs.getString("name"),
                    (Integer) rs.getObject("servings"),
                    (Integer) rs.getObject("prep_minutes"),
                    (Integer) rs.getObject("cook_minutes"),
                    rs.getBoolean("mine"),
                    total,
                    available,
                    total == 0 ? 0 : (double) available / total,
                    toList(rs.getArray("missing")),
                    null);
            })
            .list();
        Map<Long, Nutrition> nutrition = recipeService.nutritionByRecipe(
            ranked.stream().map(Recommendation::recipeId).toList());
        return ranked.stream()
            .map(r -> new Recommendation(r.recipeId(), r.name(), r.servings(), r.prepMinutes(), r.cookMinutes(),
                r.mine(), r.totalIngredients(), r.availableIngredients(), r.coverage(), r.missingIngredients(),
                nutrition.get(r.recipeId())))
            .toList();
    }

    private static List<String> toList(Array array) throws SQLException {
        if (array == null) {
            return List.of();
        }
        return Arrays.stream((Object[]) array.getArray()).map(String::valueOf).toList();
    }
}
