package com.mealplanner.recipe;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mealplanner.common.ForbiddenException;
import com.mealplanner.common.NotFoundException;
import com.mealplanner.common.PageResponse;
import com.mealplanner.food.FoodDtos.FoodSummary;
import com.mealplanner.food.FoodRepository;
import com.mealplanner.recipe.RecipeDtos.IngredientLine;
import com.mealplanner.recipe.RecipeDtos.IngredientRequest;
import com.mealplanner.recipe.RecipeDtos.Macros;
import com.mealplanner.recipe.RecipeDtos.Nutrition;
import com.mealplanner.recipe.RecipeDtos.RecipeDetail;
import com.mealplanner.recipe.RecipeDtos.RecipeRequest;
import com.mealplanner.recipe.RecipeDtos.RecipeSummary;

@Service
public class RecipeService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final RecipeRepository recipes;
    private final FoodRepository foods;
    private final IngredientMatcher matcher;

    public RecipeService(RecipeRepository recipes, FoodRepository foods, IngredientMatcher matcher) {
        this.recipes = recipes;
        this.foods = foods;
        this.matcher = matcher;
    }

    @Transactional(readOnly = true)
    public PageResponse<RecipeSummary> listMine(Long userId, String query, int page, int size) {
        return toSummaries(recipes.findOwned(userId, normalizeQuery(query), pageRequest(page, size)), userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<RecipeSummary> listPublic(Long userId, String query, int page, int size) {
        return toSummaries(
            recipes.findByVisibility(Recipe.Visibility.PUBLIC, normalizeQuery(query), pageRequest(page, size)),
            userId);
    }

    /** Valeurs nutritionnelles d'un ensemble de recettes (pour les suggestions), par identifiant. */
    @Transactional(readOnly = true)
    public Map<Long, Nutrition> nutritionByRecipe(Collection<Long> recipeIds) {
        List<Recipe> found = recipes.findAllById(recipeIds);
        Map<Long, FoodSummary> linkedFoods = linkedFoods(found);
        return found.stream().collect(Collectors.toMap(Recipe::getId, r -> nutrition(r, linkedFoods)));
    }

    /** Lecture : propriétaire, ou n'importe qui si la recette est publique. */
    @Transactional(readOnly = true)
    public RecipeDetail get(Long recipeId, Long userId) {
        Recipe recipe = find(recipeId);
        if (recipe.getVisibility() != Recipe.Visibility.PUBLIC && !recipe.isOwnedBy(userId)) {
            throw new NotFoundException("Recette introuvable : " + recipeId);
        }
        return toDetail(recipe, userId);
    }

    @Transactional
    public RecipeDetail create(Long userId, RecipeRequest request) {
        Recipe recipe = new Recipe(userId, request.name().trim(), Recipe.Source.MANUAL);
        apply(recipe, request);
        return toDetail(recipes.save(recipe), userId);
    }

    @Transactional
    public RecipeDetail update(Long recipeId, Long userId, RecipeRequest request) {
        Recipe recipe = findOwned(recipeId, userId);
        recipe.setName(request.name().trim());
        apply(recipe, request);
        return toDetail(recipes.saveAndFlush(recipe), userId);
    }

    @Transactional
    public void delete(Long recipeId, Long userId) {
        recipes.delete(findOwned(recipeId, userId));
    }

    private void apply(Recipe recipe, RecipeRequest request) {
        recipe.setDescription(request.description());
        recipe.setServings(request.servings());
        recipe.setPrepMinutes(request.prepMinutes());
        recipe.setCookMinutes(request.cookMinutes());
        recipe.setSourceUrl(request.sourceUrl());
        if (request.visibility() != null) {
            recipe.setVisibility(request.visibility());
        }
        if (request.ingredients() != null) {
            List<RecipeIngredient> lines = new ArrayList<>();
            int position = 1;
            for (IngredientRequest ingredient : request.ingredients()) {
                RecipeIngredient line = new RecipeIngredient(position++, ingredient.name().trim(),
                    ingredient.quantity(), blankToNull(ingredient.unit()), null);
                if (ingredient.foodId() != null) {
                    if (!foods.exists(ingredient.foodId())) {
                        throw new IllegalArgumentException("Aliment inconnu : " + ingredient.foodId());
                    }
                    line.linkFood(ingredient.foodId(), 1.0);
                } else {
                    matcher.match(line.getName()).ifPresent(food -> line.linkFood(food.id(), food.score()));
                }
                lines.add(line);
            }
            recipe.replaceIngredients(lines);
        }
        if (request.steps() != null) {
            recipe.replaceSteps(request.steps().stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
        }
    }

    private Recipe find(Long recipeId) {
        return recipes.findById(recipeId)
            .orElseThrow(() -> new NotFoundException("Recette introuvable : " + recipeId));
    }

    private Recipe findOwned(Long recipeId, Long userId) {
        Recipe recipe = find(recipeId);
        if (!recipe.isOwnedBy(userId)) {
            throw new ForbiddenException("Cette recette ne vous appartient pas");
        }
        return recipe;
    }

    private PageResponse<RecipeSummary> toSummaries(Page<Recipe> page, Long userId) {
        // Aliments de toute la page chargés en une requête
        Map<Long, FoodSummary> linkedFoods = linkedFoods(page.getContent());
        return PageResponse.of(page, recipe -> toSummary(recipe, userId, linkedFoods));
    }

    private RecipeSummary toSummary(Recipe recipe, Long userId, Map<Long, FoodSummary> linkedFoods) {
        return new RecipeSummary(recipe.getId(), recipe.getName(), recipe.getServings(), recipe.getPrepMinutes(),
            recipe.getCookMinutes(), recipe.getVisibility(), recipe.getSource(), recipe.getIngredients().size(),
            !recipe.getSteps().isEmpty(), recipe.isOwnedBy(userId), nutrition(recipe, linkedFoods),
            recipe.getUpdatedAt());
    }

    private Map<Long, FoodSummary> linkedFoods(Collection<Recipe> list) {
        return foods.findSummaries(list.stream()
            .flatMap(r -> r.getIngredients().stream())
            .map(RecipeIngredient::getFoodId).filter(Objects::nonNull).distinct().toList());
    }

    private RecipeDetail toDetail(Recipe recipe, Long userId) {
        Map<Long, FoodSummary> linkedFoods = linkedFoods(List.of(recipe));

        List<IngredientLine> lines = recipe.getIngredients().stream()
            .map(i -> new IngredientLine(i.getId(), i.getPosition(), i.getName(), i.getQuantity(), i.getUnit(),
                i.getRawQuantity(), i.getFoodId(),
                Optional.ofNullable(linkedFoods.get(i.getFoodId())).map(FoodSummary::name).orElse(null),
                i.getMatchScore()))
            .toList();

        return new RecipeDetail(recipe.getId(), recipe.getName(), recipe.getDescription(), recipe.getServings(),
            recipe.getPrepMinutes(), recipe.getCookMinutes(), recipe.getVisibility(), recipe.getSource(),
            recipe.getSourceUrl(), recipe.isOwnedBy(userId), lines,
            recipe.getSteps().stream().map(RecipeStep::getInstruction).toList(),
            nutrition(recipe, linkedFoods), recipe.getCreatedAt(), recipe.getUpdatedAt());
    }

    private Nutrition nutrition(Recipe recipe, Map<Long, FoodSummary> linkedFoods) {
        BigDecimal kcal = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal carbs = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        int counted = 0;
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            FoodSummary food = linkedFoods.get(ingredient.getFoodId());
            BigDecimal grams = UnitConversions.toGrams(ingredient.getQuantity(), ingredient.getUnit());
            if (food == null || grams == null) {
                continue;
            }
            BigDecimal ratio = grams.divide(HUNDRED, 6, RoundingMode.HALF_UP);
            kcal = kcal.add(scaled(food.energyKcal(), ratio));
            protein = protein.add(scaled(food.proteinG(), ratio));
            carbs = carbs.add(scaled(food.carbohydrateG(), ratio));
            fat = fat.add(scaled(food.fatG(), ratio));
            counted++;
        }
        Macros total = new Macros(kcal, protein, carbs, fat);
        Integer servings = recipe.getServings();
        Macros perServing = servings == null || servings < 1 ? null : divide(total, BigDecimal.valueOf(servings));
        return new Nutrition(round(total), perServing == null ? null : round(perServing), counted,
            recipe.getIngredients().size());
    }

    private static Macros divide(Macros macros, BigDecimal divisor) {
        Function<BigDecimal, BigDecimal> div = value -> value.divide(divisor, 6, RoundingMode.HALF_UP);
        return new Macros(div.apply(macros.energyKcal()), div.apply(macros.proteinG()),
            div.apply(macros.carbohydrateG()), div.apply(macros.fatG()));
    }

    private static Macros round(Macros macros) {
        Function<BigDecimal, BigDecimal> round = value -> value.setScale(1, RoundingMode.HALF_UP);
        return new Macros(round.apply(macros.energyKcal()), round.apply(macros.proteinG()),
            round.apply(macros.carbohydrateG()), round.apply(macros.fatG()));
    }

    private static BigDecimal scaled(BigDecimal per100g, BigDecimal ratio) {
        return per100g == null ? BigDecimal.ZERO : per100g.multiply(ratio);
    }

    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100), Sort.by(Sort.Direction.DESC, "updatedAt"));
    }

    private static String normalizeQuery(String query) {
        return query == null ? "" : query.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
