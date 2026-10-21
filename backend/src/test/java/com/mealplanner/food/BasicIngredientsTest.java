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
        assertThat(BasicIngredients.isBasicIngredient("040301", "Jambon cuit supérieur, découenné")).isTrue();
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
    void keepsOnlyRawVersionsWhereCiqualHasCookedOnes() {
        assertThat(BasicIngredients.isBasicIngredient("020101", "Carotte, crue")).isTrue();
        assertThat(BasicIngredients.isBasicIngredient("020102", "Carotte, cuite")).isFalse();
        assertThat(BasicIngredients.isBasicIngredient("0402", "Boeuf, joue, crue")).isTrue();
        assertThat(BasicIngredients.isBasicIngredient("0401", "Boeuf, joue, braisée ou bouillie/cuite à l'eau")).isFalse();
        assertThat(BasicIngredients.isBasicIngredient("0202", "Pomme de terre, sans peau, cuite au four")).isFalse();
        assertThat(BasicIngredients.isBasicIngredient("030101", "Riz blanc, cuit")).isFalse();
        assertThat(BasicIngredients.isBasicIngredient("0406", "Saumon, cru, élevage")).isTrue();
        assertThat(BasicIngredients.isBasicIngredient("0407", "Crevette, cuite")).isFalse();
        assertThat(BasicIngredients.isBasicIngredient("040307", "Quenelle nature, à cuire")).isFalse(); // quenelles exclues par groupe
        assertThat(BasicIngredients.isBasicIngredient("0406", "Saumon, à cuire")).isTrue();
        assertThat(BasicIngredients.isBasicIngredient("041001", "Oeuf, au plat, sans matière grasse")).isFalse();
        assertThat(BasicIngredients.isBasicIngredient("041001", "Oeuf dur")).isFalse();
        assertThat(BasicIngredients.isBasicIngredient("0401", "Boeuf, onglet grillé/poêlé")).isFalse();
        assertThat(BasicIngredients.isBasicIngredient("0201", "Carotte, bouillie/cuite à l'eau, croquante")).isFalse();
        assertThat(BasicIngredients.isBasicIngredient("040304", "Merguez, boeuf et mouton, cuite")).isFalse();
        assertThat(BasicIngredients.isBasicIngredient("0301", "Blé dur précuit, grains entiers, cuit")).isFalse();
        assertThat(BasicIngredients.isBasicIngredient("0304", "Semoule de blé dur")).isTrue();
        assertThat(BasicIngredients.isBasicIngredient("1006", "Ail, rôti/cuit au four")).isFalse();
        assertThat(BasicIngredients.isBasicIngredient("0302", "Pain blanc, passé au grille-pain")).isFalse();
    }

    @Test
    void keepsCookedProductsOutsideRawOnlyGroups() {
        assertThat(BasicIngredients.isBasicIngredient("040301", "Jambon cuit, choix")).isTrue();
        assertThat(BasicIngredients.isBasicIngredient("040301", "Jambon cuit supérieur, découenné")).isTrue();
        assertThat(BasicIngredients.isBasicIngredient("050301", "Comté (fromage à pâte pressée cuite)")).isTrue();
    }

    @Test
    void displayNameDropsRawMention() {
        assertThat(BasicIngredients.displayName("Tomate ronde, crue")).isEqualTo("Tomate ronde");
        assertThat(BasicIngredients.displayName("Saumon, cru, élevage")).isEqualTo("Saumon, élevage");
        assertThat(BasicIngredients.displayName("Haricot vert, crus")).isEqualTo("Haricot vert");
        assertThat(BasicIngredients.displayName("Jambon cru")).isEqualTo("Jambon cru");
        assertThat(BasicIngredients.displayName("Camembert au lait cru")).isEqualTo("Camembert au lait cru");
        assertThat(BasicIngredients.displayName("Boeuf, côte crue")).isEqualTo("Boeuf, côte");
        assertThat(BasicIngredients.displayName("Poulet, viande et peau crues bio")).isEqualTo("Poulet, viande et peau bio");
        assertThat(BasicIngredients.displayName("Pomme, chair sans peau, crue (aliment moyen)"))
            .isEqualTo("Pomme, chair sans peau (aliment moyen)");
        assertThat(BasicIngredients.displayName("Oeuf cru")).isEqualTo("Oeuf");
        assertThat(BasicIngredients.displayName("Ananas, chair sans peau, cru , prélevé à la Martinique"))
            .isEqualTo("Ananas, chair sans peau, prélevé à la Martinique");
        assertThat(BasicIngredients.displayName("Jambon cru fumé")).isEqualTo("Jambon cru fumé");
    }

    @Test
    void groupPrefixesOnlyMatchFromTheStart() {
        // "01" (plats composés) ne doit pas exclure "0201" (légumes) ni "1001" (déjà exclu pour lui-même)
        assertThat(BasicIngredients.isBasicIngredient("020110", "Carotte, crue")).isTrue();
        assertThat(BasicIngredients.SQL_PREDICATE).contains("'^(01|");
    }
}
