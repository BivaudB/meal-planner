package com.mealplanner.importing.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.mealplanner.importing.parser.RecipeSheetParser.ParseResult;
import com.mealplanner.importing.parser.RecipeSheetParser.ParsedRecipe;

class RecipeSheetParserTest {

    private static List<String> row(String... cells) {
        return Arrays.asList(cells);
    }

    @Test
    void readsTwoRowsPerRecipe() {
        ParseResult result = RecipeSheetParser.parse(List.of(
            row("Omelette", "Oeufs", "Lait", "Sel"),
            row(null, "3", "5 cl", "1 pincée"),
            row(),
            row("Salade de tomates", "Tomates", "Huile d'olive"),
            row("", "4", "2 c. à soupe")));

        assertThat(result.recipes()).hasSize(2);
        assertThat(result.warnings()).isEmpty();

        ParsedRecipe omelette = result.recipes().getFirst();
        assertThat(omelette.name()).isEqualTo("Omelette");
        assertThat(omelette.sourceRow()).isEqualTo(1);
        assertThat(omelette.ingredients()).extracting(i -> i.name()).containsExactly("Oeufs", "Lait", "Sel");
        assertThat(omelette.ingredients().get(1).quantity().quantity()).isEqualByComparingTo(new BigDecimal("5"));
        assertThat(omelette.ingredients().get(1).quantity().unit()).isEqualTo("cl");

        ParsedRecipe salade = result.recipes().get(1);
        assertThat(salade.ingredients().get(1).quantity().unit()).isEqualTo("cas");
    }

    @Test
    void ignoresPlaceholderRowsOfTheExampleFile() {
        // Structure du fichier Exemple_import.xlsx fourni
        ParseResult result = RecipeSheetParser.parse(List.of(
            row("Recette 1", "Ingredient 1", "Ingredient 2", "Ingredient 3", "…"),
            row(null, "Quantite 1", "Quantite 2", "Quantite 3", "…"),
            row("Recette 2", "Ingredient 1", "Ingredient 2", "Ingredient 3", "Ingredient 4", "…"),
            row(null, "Quantite 1", "Quantite 2", "Quantite 3", "Quantite 4", "…"),
            row("…", "…", "…", "…", "…"),
            row(null, "…", "…", "…", "…")));

        assertThat(result.recipes()).extracting(ParsedRecipe::name).containsExactly("Recette 1", "Recette 2");
        assertThat(result.recipes().get(1).ingredients()).hasSize(4);
    }

    @Test
    void reportsRecipeWithoutQuantityRow() {
        ParseResult result = RecipeSheetParser.parse(List.of(
            row("Pâtes au beurre", "Pâtes", "Beurre"),
            row("Riz", "Riz"),
            row(null, "100 g")));

        assertThat(result.recipes()).hasSize(2);
        assertThat(result.recipes().getFirst().ingredients().getFirst().quantity().quantity()).isNull();
        assertThat(result.warnings()).singleElement().asString().contains("Pâtes au beurre");
    }
}
