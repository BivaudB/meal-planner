package com.mealplanner.food;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BasicIngredientsTest {

    @Test
    void keepsRawProducts() {
        assertThat(BasicIngredients.isBasicIngredient("020101", "Tomate ronde, crue")).isTrue();
        assertThat(BasicIngredients.isBasicIngredient("041002", "Oeuf, cru")).isTrue();
        assertThat(BasicIngredients.isBasicIngredient("0304", "Farine de blé tendre ou froment T55")).isTrue();
        assertThat(BasicIngredients.isBasicIngredient("0305", "Pâte feuilletée, pur beurre, crue")).isTrue();
        assertThat(BasicIngredients.isBasicIngredient("0409", "Thon, au naturel, appertisé, égoutté")).isTrue();
        assertThat(BasicIngredients.isBasicIngredient("040301", "Jambon cuit, choix")).isTrue();
        assertThat(BasicIngredients.isBasicIngredient("0201", "Panais, cru")).isTrue();
    }

    @Test
    void excludesDishesByGroup() {
        assertThat(BasicIngredients.isBasicIngredient("0102", "Soupe de poisson")).isFalse();
        assertThat(BasicIngredients.isBasicIngredient("010301", "Lasagnes à la bolognaise")).isFalse();
        assertThat(BasicIngredients.isBasicIngredient("0709", "Gâteau au chocolat")).isFalse();
        assertThat(BasicIngredients.isBasicIngredient("1001", "Sauce béarnaise, préemballée")).isFalse();
        assertThat(BasicIngredients.isBasicIngredient("040305", "Pâté de campagne")).isFalse();
        assertThat(BasicIngredients.isBasicIngredient(null, "Inconnu")).isFalse();
    }

    @Test
    void excludesPreparationsByName() {
        assertThat(BasicIngredients.isBasicIngredient("0409", "Accra de poisson, préemballé")).isFalse();
        assertThat(BasicIngredients.isBasicIngredient("0409", "Poisson pané, frit")).isFalse();
        assertThat(BasicIngredients.isBasicIngredient("0409", "Langoustine, panée, frite")).isFalse();
        assertThat(BasicIngredients.isBasicIngredient("041003", "Omelette nature, faite maison")).isFalse();
    }

    @Test
    void groupPrefixesOnlyMatchFromTheStart() {
        // "01" (plats composés) ne doit pas exclure "0201" (légumes) ni "1001" (déjà exclu pour lui-même)
        assertThat(BasicIngredients.isBasicIngredient("020110", "Carotte, crue")).isTrue();
        assertThat(BasicIngredients.SQL_PREDICATE).contains("'^(01|");
    }
}
