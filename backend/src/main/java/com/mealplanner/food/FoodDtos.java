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

    /** Ligne du catalogue d'aliments : catégorie (groupe Ciqual) et sous-catégorie (sous-groupe). */
    public record FoodCatalogItem(
        Long id,
        String name,
        String categoryCode,
        String categoryName,
        String subcategoryCode,
        String subcategoryName,
        BigDecimal energyKcal,
        BigDecimal proteinG,
        BigDecimal carbohydrateG,
        BigDecimal fatG) {
    }

    /** Critères de tri du catalogue (liste blanche, convertie en colonne SQL par le dépôt). */
    public enum CatalogSort {
        NAME, ENERGY_KCAL, PROTEIN_G, CARBOHYDRATE_G, FAT_G
    }

    /** Groupe Ciqual (alim_grp_nom_fr) et ses sous-groupes (alim_ssgrp_nom_fr), avec le nombre d'aliments. */
    public record FoodCategory(String code, String name, long foodCount, List<FoodSubcategory> subcategories) {
    }

    public record FoodSubcategory(String code, String name, long foodCount) {
    }

    /** Valeur d'un nutriment pour 100 g. {@code qualifier} vaut "<" ou "traces" le cas échéant. */
    public record NutrientValue(Integer id, String name, String unit, BigDecimal value, String qualifier, String rawValue) {
    }
}
