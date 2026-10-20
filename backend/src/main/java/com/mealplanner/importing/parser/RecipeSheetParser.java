package com.mealplanner.importing.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.mealplanner.importing.parser.QuantityParser.ParsedQuantity;

/**
 * Lit des recettes au format "deux lignes par recette" (Excel ou CSV) :
 *
 * <pre>
 * | Omelette | Oeufs | Lait  | Sel       |
 * |          | 3     | 5 cl  | 1 pincée  |
 * | Salade   | ...                       |
 * </pre>
 *
 * Ligne 1 : nom de la recette dans la 1re colonne, puis un ingrédient par colonne.
 * Ligne 2 : 1re colonne vide, puis la quantité de chaque ingrédient, alignée en colonne.
 * Les lignes vides séparant les recettes sont ignorées.
 */
public final class RecipeSheetParser {

    private static final Set<String> PLACEHOLDERS = Set.of("…", "...", "-");

    public record ParsedIngredient(int position, String name, ParsedQuantity quantity) {
    }

    public record ParsedRecipe(int sourceRow, String name, List<ParsedIngredient> ingredients) {
    }

    public record ParseResult(List<ParsedRecipe> recipes, List<String> warnings) {
    }

    private RecipeSheetParser() {
    }

    /**
     * @param rows contenu du fichier, ligne par ligne ; une cellule vide vaut null ou "".
     */
    public static ParseResult parse(List<List<String>> rows) {
        List<ParsedRecipe> recipes = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        int i = 0;
        while (i < rows.size()) {
            List<String> row = rows.get(i);
            int lineNumber = i + 1;
            if (isBlankRow(row)) {
                i++;
                continue;
            }
            String recipeName = clean(cell(row, 0));
            if (recipeName == null) {
                warnings.add("Ligne " + lineNumber + " : ligne de quantités sans recette au-dessus, ignorée");
                i++;
                continue;
            }

            List<String> quantities = null;
            if (i + 1 < rows.size() && !isBlankRow(rows.get(i + 1)) && clean(cell(rows.get(i + 1), 0)) == null) {
                quantities = rows.get(i + 1);
                i += 2;
            } else {
                warnings.add("Ligne " + lineNumber + " : recette « " + recipeName + " » sans ligne de quantités");
                i++;
            }

            List<ParsedIngredient> ingredients = new ArrayList<>();
            int width = Math.max(row.size(), quantities == null ? 0 : quantities.size());
            for (int col = 1; col < width; col++) {
                String ingredientName = clean(cell(row, col));
                String quantityText = quantities == null ? null : clean(cell(quantities, col));
                if (ingredientName == null) {
                    if (quantityText != null) {
                        warnings.add("Ligne " + (lineNumber + 1) + ", colonne " + (col + 1)
                            + " : quantité « " + quantityText + " » sans ingrédient, ignorée");
                    }
                    continue;
                }
                ingredients.add(new ParsedIngredient(ingredients.size() + 1, ingredientName,
                    QuantityParser.parse(quantityText)));
            }

            if (ingredients.isEmpty()) {
                warnings.add("Ligne " + lineNumber + " : recette « " + recipeName + " » sans ingrédient, ignorée");
                continue;
            }
            recipes.add(new ParsedRecipe(lineNumber, recipeName, ingredients));
        }
        return new ParseResult(recipes, warnings);
    }

    private static String cell(List<String> row, int index) {
        return row != null && index < row.size() ? row.get(index) : null;
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.replace(' ', ' ').trim();
        return trimmed.isEmpty() || PLACEHOLDERS.contains(trimmed) ? null : trimmed;
    }

    private static boolean isBlankRow(List<String> row) {
        return row == null || row.stream().allMatch(value -> clean(value) == null);
    }
}
