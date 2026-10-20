package com.mealplanner.recipe;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Conversion approximative d'une quantité en grammes, pour les calculs nutritionnels.
 * Les volumes sont convertis avec une densité de 1 (eau) : suffisant pour une estimation.
 * Les unités sans équivalence fiable (pièce, gousse, tranche...) renvoient null : il faudra
 * plus tard un poids moyen par aliment (ex. 1 œuf ≈ 50 g).
 */
public final class UnitConversions {

    private static final Map<String, BigDecimal> GRAMS_PER_UNIT = Map.ofEntries(
        Map.entry("g", BigDecimal.ONE),
        Map.entry("kg", new BigDecimal("1000")),
        Map.entry("mg", new BigDecimal("0.001")),
        Map.entry("ml", BigDecimal.ONE),
        Map.entry("cl", BigDecimal.TEN),
        Map.entry("dl", new BigDecimal("100")),
        Map.entry("l", new BigDecimal("1000")),
        Map.entry("cas", new BigDecimal("15")),
        Map.entry("cac", new BigDecimal("5")),
        Map.entry("pincee", new BigDecimal("0.5")),
        Map.entry("verre", new BigDecimal("200")));

    private UnitConversions() {
    }

    public static BigDecimal toGrams(BigDecimal quantity, String unit) {
        if (quantity == null || unit == null) {
            return null;
        }
        BigDecimal factor = GRAMS_PER_UNIT.get(unit);
        return factor == null ? null : quantity.multiply(factor);
    }
}
