package com.mealplanner.importing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.mealplanner.food.FoodDtos.FoodSummary;
import com.mealplanner.importing.parser.RecipeSheetParser;
import com.mealplanner.importing.parser.RecipeSheetParser.ParseResult;
import com.mealplanner.importing.parser.RecipeSheetParser.ParsedIngredient;
import com.mealplanner.importing.parser.RecipeSheetParser.ParsedRecipe;
import com.mealplanner.importing.parser.TabularFileReader;
import com.mealplanner.recipe.IngredientMatcher;
import com.mealplanner.recipe.Recipe;
import com.mealplanner.recipe.RecipeIngredient;
import com.mealplanner.recipe.RecipeRepository;

/**
 * Import de recettes depuis un fichier Excel/CSV, en tâche de fond.
 * Chaque recette est enregistrée dans sa propre transaction : une recette en erreur
 * n'empêche pas l'import des autres, et le rapport liste les problèmes rencontrés.
 */
@Service
public class RecipeImportService {

    private static final Logger log = LoggerFactory.getLogger(RecipeImportService.class);
    private static final int MAX_REPORT_LINES = 200;

    private final ImportJobRepository jobs;
    private final RecipeRepository recipes;
    private final IngredientMatcher matcher;
    private final TransactionTemplate transaction;

    public RecipeImportService(ImportJobRepository jobs, RecipeRepository recipes, IngredientMatcher matcher,
                               TransactionTemplate transaction) {
        this.jobs = jobs;
        this.recipes = recipes;
        this.matcher = matcher;
        this.transaction = transaction;
    }

    /** Crée le suivi de l'import ; le traitement est lancé ensuite par {@link #process}. */
    public ImportJob createJob(Long userId, String filename) {
        return jobs.save(new ImportJob(userId, ImportJob.Type.RECIPES, filename));
    }

    @Async
    public void process(Long jobId, byte[] content) {
        ImportJob job = jobs.findById(jobId).orElseThrow();
        job.start();
        job = jobs.save(job);
        try {
            List<List<String>> rows = TabularFileReader.read(job.getFilename(), content);
            ParseResult parsed = RecipeSheetParser.parse(rows);
            List<String> report = new ArrayList<>(parsed.warnings());
            Map<String, Optional<FoodSummary>> matchCache = IngredientMatcher.newCache();

            int imported = 0;
            int errors = 0;
            int unmatched = 0;
            for (ParsedRecipe parsedRecipe : parsed.recipes()) {
                try {
                    Recipe recipe = toRecipe(job.getUserId(), parsedRecipe, matchCache);
                    transaction.executeWithoutResult(status -> recipes.save(recipe));
                    imported++;
                    unmatched += (int) recipe.getIngredients().stream().filter(i -> i.getFoodId() == null).count();
                } catch (RuntimeException e) {
                    errors++;
                    report.add("Ligne " + parsedRecipe.sourceRow() + " (« " + parsedRecipe.name() + " ») : "
                        + e.getMessage());
                }
            }
            if (unmatched > 0) {
                report.add(unmatched + " ingrédient(s) sans correspondance dans le référentiel Ciqual "
                    + "(à rattacher manuellement depuis la fiche recette).");
            }
            job.finish(parsed.recipes().size(), imported, errors, toReport(report));
        } catch (IOException | RuntimeException e) {
            log.warn("Échec de l'import {} : {}", jobId, e.getMessage());
            job.fail("Fichier illisible : " + e.getMessage());
        }
        jobs.save(job);
    }

    private Recipe toRecipe(Long userId, ParsedRecipe parsed, Map<String, Optional<FoodSummary>> matchCache) {
        if (parsed.name().length() > 255) {
            throw new IllegalArgumentException("nom de recette trop long (255 caractères maximum)");
        }
        Recipe recipe = new Recipe(userId, parsed.name(), Recipe.Source.IMPORT);
        for (ParsedIngredient parsedIngredient : parsed.ingredients()) {
            String rawQuantity = parsedIngredient.quantity().raw();
            RecipeIngredient ingredient = new RecipeIngredient(
                parsedIngredient.position(),
                truncate(parsedIngredient.name(), 255),
                parsedIngredient.quantity().quantity(),
                parsedIngredient.quantity().unit(),
                truncate(rawQuantity, 100));
            matcher.match(parsedIngredient.name(), matchCache)
                .ifPresent(food -> ingredient.linkFood(food.id(), food.score()));
            recipe.addIngredient(ingredient);
        }
        return recipe;
    }

    private static String toReport(List<String> lines) {
        if (lines.isEmpty()) {
            return null;
        }
        List<String> kept = lines.size() > MAX_REPORT_LINES ? lines.subList(0, MAX_REPORT_LINES) : lines;
        String report = String.join("\n", kept);
        return lines.size() > MAX_REPORT_LINES
            ? report + "\n… et " + (lines.size() - MAX_REPORT_LINES) + " autre(s) message(s)"
            : report;
    }

    private static String truncate(String text, int max) {
        return text == null || text.length() <= max ? text : text.substring(0, max);
    }
}
