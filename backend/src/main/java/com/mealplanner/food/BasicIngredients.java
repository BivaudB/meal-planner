package com.mealplanner.food;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Définit les « ingrédients de base » : les aliments Ciqual que l'on achète pour cuisiner
 * (oeufs, tomates, farine, beurre…), par opposition aux plats et produits prêts à consommer
 * (soupes, pizzas, gâteaux, sodas, sauces industrielles, terrines…). Seuls ces ingrédients sont
 * chargés en base par l'import Ciqual (la migration V4 a appliqué le même filtre aux bases existantes).
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
     */
    public static final List<String> EXCLUDED_NAME_PATTERNS = List.of(
        "^(accra|beignet|nuggets|croquette|tarama|terrine|rillettes|omelette)( |,|$)",
        "faites? maison|faits? maison",
        "(^|[ ,])panee?s?([ ,]|$)"
    );

    /**
     * Groupes où Ciqual décline un même produit cru et cuit (légumes, céréales, viandes,
     * poissons, oeufs) : seule la version crue est gardée. Ailleurs, « cuit » désigne un
     * produit en soi (jambon cuit, fromage à pâte pressée cuite) et n'est pas filtré.
     */
    public static final List<String> RAW_ONLY_GROUP_PREFIXES = List.of(
        "02", "03", "0401", "0402", "0403", "0405", "0406", "0407", "0408", "0409", "0410", "10");

    /**
     * Modes de cuisson, sur le nom normalisé (Ciqual écrit aussi « grillée/poêlée »).
     * « à cuire » (produit cru) n'est pas concerné.
     */
    public static final String COOKED_PATTERN = "(^|[ ,(/])("
        + "cuite?s?|precuite?s?|bouillie?s?|braisee?s?|grillee?s?|rotie?s?|poelee?s?|sautee?s?|frite?s?"
        + "|prefrite?s?|etouffee?s?|rissolee?s?|cuisinee?s?|mijotee?s?|pochee?s?|brouillee?s?|mollet"
        + "|au four|vapeur|au plat|a la coque|oeufs? durs?|au grille-pain"
        + ")([ ,)/]|$)";

    /** Produits dont la cuisson fait partie de l'identité (« Jambon cuit ») : jamais filtrés. */
    private static final String COOKED_PRODUCT_PATTERN = "^jambon";

    /**
     * Mention « cru » (« Tomate ronde, crue », « Boeuf, côte crue ») ; « Jambon cru » et
     * « au lait cru » désignent un produit et sont conservés. À appliquer sans tenir compte de la casse.
     */
    private static final String RAW_MENTION_PATTERN = "(?<!lait|jambon)(\\s*,\\s*|\\s+)crue?s?(\\s+(?=,))?(?=[\\s,(]|$)";

    private static final Pattern NAME_PATTERN = Pattern.compile(String.join("|", EXCLUDED_NAME_PATTERNS));
    private static final Pattern COOKED = Pattern.compile(COOKED_PATTERN);
    private static final Pattern COOKED_PRODUCT = Pattern.compile(COOKED_PRODUCT_PATTERN);
    private static final Pattern RAW_MENTION = Pattern.compile(RAW_MENTION_PATTERN, Pattern.CASE_INSENSITIVE);
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private BasicIngredients() {
    }

    /** Vrai si l'aliment est un ingrédient de base, à garder lors de l'import Ciqual. */
    public static boolean isBasicIngredient(String groupCode, String name) {
        if (groupCode == null || EXCLUDED_GROUP_PREFIXES.stream().anyMatch(groupCode::startsWith)) {
            return false;
        }
        String normalized = DIACRITICS.matcher(Normalizer.normalize(name, Normalizer.Form.NFD)).replaceAll("")
            .toLowerCase(Locale.ROOT).replace("œ", "oe").replace("æ", "ae");
        if (NAME_PATTERN.matcher(normalized).find()) {
            return false;
        }
        boolean rawOnly = RAW_ONLY_GROUP_PREFIXES.stream().anyMatch(groupCode::startsWith);
        return !(rawOnly && COOKED.matcher(normalized).find() && !COOKED_PRODUCT.matcher(normalized).find());
    }

    /** Nom enregistré d'un ingrédient de base : « Tomate ronde, crue » → « Tomate ronde ». */
    public static String displayName(String name) {
        return RAW_MENTION.matcher(name).replaceAll("");
    }
}
