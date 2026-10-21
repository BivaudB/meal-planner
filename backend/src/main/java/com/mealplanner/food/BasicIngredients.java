package com.mealplanner.food;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Définit les « ingrédients de base » : les aliments Ciqual que l'on achète pour cuisiner
 * (oeufs, tomates, farine, beurre…), par opposition aux plats et produits prêts à consommer
 * (soupes, pizzas, gâteaux, sodas, sauces industrielles, terrines…).
 * <p>
 * Le tri se fait d'abord par groupe Ciqual : les codes sont hiérarchiques (01, 0101, 010101),
 * un préfixe exclut donc tout ce qui se trouve en dessous. Certains sous-groupes mélangent
 * produits bruts et préparations (ex. « produits à base de poissons » : thon au naturel mais
 * aussi accras) : quelques motifs sur le libellé complètent le filtre.
 */
public final class BasicIngredients {

    /** Groupes (2 chiffres), sous-groupes (4) et sous-sous-groupes (6) exclus. */
    public static final List<String> EXCLUDED_GROUP_PREFIXES = List.of(
        "01",     // entrées et plats composés (salades, soupes, plats, pizzas, sandwichs…)
        "0303",   // biscuits apéritifs
        "040305", // pâtés et terrines
        "040306", // rillettes
        "040307", // quenelles
        "040308", // autres spécialités charcutières
        "0404",   // autres produits à base de viande
        "050203", // desserts lactés
        "050204", // autres desserts
        "050205", // desserts végétaux
        "06",     // eaux et boissons (le vin de cuisine est dans les aides culinaires)
        "0703",   // confiseries non chocolatées
        "0705",   // viennoiseries
        "0706",   // biscuits sucrés
        "0707",   // céréales de petit-déjeuner
        "0708",   // barres céréalières
        "0709",   // gâteaux et pâtisseries
        "08",     // glaces et sorbets
        "1001",   // sauces
        "1008",   // alimentation particulière (substituts de repas)
        "1010",   // tartinables végétariens
        "11"      // aliments infantiles
    );

    /**
     * Libellés de préparations, testés sur le nom normalisé (minuscules, sans accents).
     * Syntaxe commune aux expressions régulières Java et PostgreSQL.
     */
    public static final List<String> EXCLUDED_NAME_PATTERNS = List.of(
        "^(accra|beignet|nuggets|croquette|tarama|terrine|rillettes|omelette)( |,|$)",
        "faites? maison|faits? maison",
        "(^|[ ,])panee?s?([ ,]|$)"
    );

    /** Condition SQL sur l'alias {@code f} de la table food. */
    static final String SQL_PREDICATE = "f.group_code IS NOT NULL"
        + " AND f.group_code !~ '^(" + String.join("|", EXCLUDED_GROUP_PREFIXES) + ")'"
        + " AND f_normalize(f.name) !~ '" + String.join("|", EXCLUDED_NAME_PATTERNS) + "'";

    private static final Pattern NAME_PATTERN = Pattern.compile(String.join("|", EXCLUDED_NAME_PATTERNS));
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private BasicIngredients() {
    }

    /** Équivalent Java de {@link #SQL_PREDICATE}. */
    public static boolean isBasicIngredient(String groupCode, String name) {
        if (groupCode == null || EXCLUDED_GROUP_PREFIXES.stream().anyMatch(groupCode::startsWith)) {
            return false;
        }
        String normalized = DIACRITICS.matcher(Normalizer.normalize(name, Normalizer.Form.NFD)).replaceAll("")
            .toLowerCase(Locale.ROOT);
        return !NAME_PATTERN.matcher(normalized).find();
    }
}
