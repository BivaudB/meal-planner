package com.mealplanner.food.ciqual;

import java.math.BigDecimal;

/**
 * Interprète une valeur de la table Ciqual. Formats rencontrés :
 * <ul>
 *   <li>{@code "49,9"} : valeur numérique (virgule décimale) ;</li>
 *   <li>{@code "< 0,2"} : inférieure à la limite de quantification (qualifier "&lt;") ;</li>
 *   <li>{@code "traces"} : présence en quantité négligeable (valeur 0, qualifier "traces") ;</li>
 *   <li>{@code "-"} ou vide : non renseigné (valeur null).</li>
 * </ul>
 */
public final class CiqualValueParser {

    public static final String LESS_THAN = "<";
    public static final String TRACES = "traces";

    private CiqualValueParser() {
    }

    public record ParsedValue(BigDecimal value, String qualifier, String raw) {

        /** Valeur à utiliser dans les calculs : 0 pour "traces" et "&lt; x", sinon la valeur. */
        public BigDecimal forCalculation() {
            if (qualifier != null) {
                return BigDecimal.ZERO;
            }
            return value;
        }

        public boolean isEmpty() {
            return value == null && qualifier == null;
        }
    }

    public static ParsedValue parse(String rawInput) {
        if (rawInput == null) {
            return new ParsedValue(null, null, null);
        }
        String raw = rawInput.replaceAll("\\s+", " ").trim();
        if (raw.isEmpty() || raw.equals("-")) {
            return new ParsedValue(null, null, raw.isEmpty() ? null : raw);
        }
        if (raw.equalsIgnoreCase("traces")) {
            return new ParsedValue(BigDecimal.ZERO, TRACES, raw);
        }
        if (raw.startsWith("<")) {
            BigDecimal limit = toDecimal(raw.substring(1));
            return new ParsedValue(limit, LESS_THAN, raw);
        }
        return new ParsedValue(toDecimal(raw), null, raw);
    }

    /** "1 070,5" -> 1070.5 ; renvoie null si le texte n'est pas un nombre. */
    public static BigDecimal toDecimal(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.replace(' ', ' ').replace(" ", "").replace(',', '.').trim();
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
