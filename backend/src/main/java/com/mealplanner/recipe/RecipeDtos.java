package com.mealplanner.recipe;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Objets échangés par l'API des recettes. */
public final class RecipeDtos {

    private RecipeDtos() {
    }

    public record RecipeSummary(
        Long id,
        String name,
        Integer servings,
        Integer prepMinutes,
        Integer cookMinutes,
        Recipe.Visibility visibility,
        Recipe.Source source,
        int ingredientCount,
        boolean hasSteps,
        boolean mine,
        Nutrition nutrition,
        Instant updatedAt) {
    }

    public record RecipeDetail(
        Long id,
        String name,
        String description,
        Integer servings,
        Integer prepMinutes,
        Integer cookMinutes,
        Recipe.Visibility visibility,
        Recipe.Source source,
        String sourceUrl,
        boolean mine,
        List<IngredientLine> ingredients,
        List<String> steps,
        Nutrition nutrition,
        Instant createdAt,
        Instant updatedAt) {
    }

    public record IngredientLine(
        Long id,
        int position,
        String name,
        BigDecimal quantity,
        String unit,
        String rawQuantity,
        Long foodId,
        String foodName,
        Double matchScore) {
    }

    /** Les quatre valeurs mises en avant partout : calories, protéines, glucides, lipides. */
    public record Macros(
        BigDecimal energyKcal,
        BigDecimal proteinG,
        BigDecimal carbohydrateG,
        BigDecimal fatG) {
    }

    /**
     * Estimation nutritionnelle, calculée sur les ingrédients dont la quantité peut être
     * convertie en grammes et qui sont rattachés à Ciqual. {@code perServing} est null si
     * le nombre de personnes n'est pas renseigné.
     */
    public record Nutrition(
        Macros total,
        Macros perServing,
        int ingredientsCounted,
        int ingredientsTotal) {
    }

    public record RecipeRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 10_000) String description,
        @Min(1) @Max(100) Integer servings,
        @Min(0) @Max(10_000) Integer prepMinutes,
        @Min(0) @Max(10_000) Integer cookMinutes,
        Recipe.Visibility visibility,
        @Size(max = 1000) String sourceUrl,
        @Valid @Size(max = 200) List<IngredientRequest> ingredients,
        @Size(max = 100) List<@NotBlank @Size(max = 5_000) String> steps) {
    }

    public record IngredientRequest(
        @NotBlank @Size(max = 255) String name,
        BigDecimal quantity,
        @Size(max = 30) String unit,
        Long foodId) {
    }
}
