package com.mealplanner.food;

import java.math.BigDecimal;
import java.util.List;

/** Objets renvoyés par l'API du référentiel d'aliments. */
public final class FoodDtos {

    private FoodDtos() {
    }

    /** Résultat de recherche : valeurs clés pour 100 g. */
    public record FoodSummary(
        Long id,
        String name,
        String groupName,
        BigDecimal energyKcal,
        BigDecimal proteinG,
        BigDecimal carbohydrateG,
        BigDecimal fatG,
        Double score) {
    }

    public record FoodDetail(
        Long id,
        String name,
        String scientificName,
        String groupName,
        BigDecimal energyKcal,
        BigDecimal energyKj,
        BigDecimal proteinG,
        BigDecimal carbohydrateG,
        BigDecimal sugarsG,
        BigDecimal fatG,
        BigDecimal saturatedFatG,
        BigDecimal fiberG,
        BigDecimal saltG,
        List<NutrientValue> nutrients) {
    }

    /** Valeur d'un nutriment pour 100 g. {@code qualifier} vaut "<" ou "traces" le cas échéant. */
    public record NutrientValue(Integer id, String name, String unit, BigDecimal value, String qualifier, String rawValue) {
    }
}
