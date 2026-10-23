package com.mealplanner.importing;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.mealplanner.config.AppProperties;
import com.mealplanner.food.FoodService;
import com.mealplanner.importing.parser.QuantityParser;
import com.mealplanner.importing.parser.QuantityParser.ParsedQuantity;
import com.mealplanner.recipe.IngredientMatcher;
import com.mealplanner.recipe.Recipe;
import com.mealplanner.recipe.RecipeIngredient;
import com.mealplanner.recipe.RecipeRepository;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Au démarrage, ajoute les recettes de base (resources/seed/recipes.json) en recettes
 * publiques de l'utilisateur technique "system:catalog" (migration V2). Les recettes déjà
 * présentes (même nom) ne sont pas modifiées : compléter le fichier suffit pour en ajouter.
 * S'exécute après l'import Ciqual, dont il a besoin pour rattacher les ingrédients.
 */
@Component
@Order(2)
public class RecipeCatalogSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RecipeCatalogSeeder.class);
    private static final String CATALOG_USER = "system:catalog";
    private static final String CATALOG_FILE = "seed/recipes.json";

    /** Recette du fichier ; {@code ciqual} = code alim_code de l'aliment (prioritaire sur le nom). */
    record CatalogRecipe(String name, String description, Integer servings, Integer prepMinutes,
                         Integer cookMinutes, List<CatalogIngredient> ingredients, List<String> steps) {
    }

    record CatalogIngredient(String name, String quantity, Long ciqual) {
    }

    private final AppProperties properties;
    private final JdbcClient jdbc;
    private final FoodService foods;
    private final RecipeRepository recipes;
    private final IngredientMatcher matcher;
    private final TransactionTemplate transaction;
    private final ObjectMapper json;

    public RecipeCatalogSeeder(AppProperties properties, JdbcClient jdbc, FoodService foods,
                               RecipeRepository recipes, IngredientMatcher matcher,
                               TransactionTemplate transaction, ObjectMapper json) {
        this.properties = properties;
        this.jdbc = jdbc;
        this.foods = foods;
        this.recipes = recipes;
        this.matcher = matcher;
        this.transaction = transaction;
        this.json = json;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (!properties.catalog().enabled()) {
            return;
        }
        if (foods.count() == 0) {
            log.warn("Référentiel Ciqual vide : catalogue de recettes non chargé (il le sera au prochain démarrage)");
            return;
        }
        Long ownerId = jdbc.sql("SELECT id FROM app_user WHERE external_id = :id")
            .param("id", CATALOG_USER).query(Long.class).single();
        Set<String> existing = new HashSet<>(jdbc.sql("SELECT name FROM recipe WHERE owner_id = :owner")
            .param("owner", ownerId).query(String.class).list());

        List<CatalogRecipe> catalog;
        try (InputStream input = new ClassPathResource(CATALOG_FILE).getInputStream()) {
            catalog = json.readValue(input, new TypeReference<List<CatalogRecipe>>() { });
        }

        int added = 0;
        for (CatalogRecipe entry : catalog) {
            if (existing.contains(entry.name())) {
                continue;
            }
            Recipe recipe = toRecipe(ownerId, entry);
            transaction.executeWithoutResult(status -> recipes.save(recipe));
            added++;
        }
        if (added > 0) {
            log.info("Catalogue de recettes : {} recette(s) ajoutée(s)", added);
        }
    }

    private Recipe toRecipe(Long ownerId, CatalogRecipe entry) {
        Recipe recipe = new Recipe(ownerId, entry.name(), Recipe.Source.CATALOG);
        recipe.setDescription(entry.description());
        recipe.setServings(entry.servings());
        recipe.setPrepMinutes(entry.prepMinutes());
        recipe.setCookMinutes(entry.cookMinutes());
        recipe.setVisibility(Recipe.Visibility.PUBLIC);
        int position = 1;
        for (CatalogIngredient item : entry.ingredients()) {
            ParsedQuantity quantity = QuantityParser.parse(item.quantity());
            RecipeIngredient ingredient = new RecipeIngredient(position++, item.name(), quantity.quantity(),
                quantity.unit(), quantity.raw());
            if (item.ciqual() != null && foods.exists(item.ciqual())) {
                ingredient.linkFood(item.ciqual(), 1.0);
            } else {
                // Code absent ou disparu d'une nouvelle version de Ciqual : recherche par le nom
                matcher.match(item.name()).ifPresent(food -> ingredient.linkFood(food.id(), food.score()));
            }
            recipe.addIngredient(ingredient);
        }
        recipe.replaceSteps(entry.steps() == null ? List.of() : entry.steps());
        return recipe;
    }
}
