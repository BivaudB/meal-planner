package com.mealplanner.recipe;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.mealplanner.config.AppProperties;
import com.mealplanner.food.FoodDtos.FoodSummary;
import com.mealplanner.food.FoodService;

/**
 * Rattache un nom d'ingrédient libre ("tomates", "Farine T55") à un aliment Ciqual
 * par recherche approximative. Le rattachement reste modifiable dans l'application.
 */
@Component
public class IngredientMatcher {

    private final FoodService foods;
    private final double threshold;

    public IngredientMatcher(FoodService foods, AppProperties properties) {
        this.foods = foods;
        this.threshold = properties.importing().matchThreshold();
    }

    public Optional<FoodSummary> match(String ingredientName) {
        return foods.bestMatch(ingredientName, threshold);
    }

    /** Variante avec cache, pour les imports où les mêmes ingrédients reviennent souvent. */
    public Optional<FoodSummary> match(String ingredientName, Map<String, Optional<FoodSummary>> cache) {
        return cache.computeIfAbsent(ingredientName.toLowerCase().trim(), key -> match(ingredientName));
    }

    public static Map<String, Optional<FoodSummary>> newCache() {
        return new HashMap<>();
    }
}
