package com.mealplanner.importing.parser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interprète une quantité saisie librement : "200 g", "200g", "1,5 kg", "2 c. à soupe",
 * "1/2", "3", "une pincée", "20 cl"...
 * <p>
 * Résultat : une valeur numérique (éventuellement nulle) et une unité normalisée
 * (g, kg, mg, ml, cl, dl, l, cas, cac, pincee, piece, ...). Le texte d'origine est conservé.
 */
public final class QuantityParser {

    public record ParsedQuantity(BigDecimal quantity, String unit, String raw) {
    }

    private static final Pattern NUMBER_THEN_UNIT = Pattern.compile(
        "^(\\d+(?:[.,]\\d+)?)(?:\\s*/\\s*(\\d+))?\\s*(.*)$");

    private static final Map<String, BigDecimal> WORD_NUMBERS = Map.of(
        "un", BigDecimal.ONE, "une", BigDecimal.ONE, "deux", BigDecimal.valueOf(2),
        "trois", BigDecimal.valueOf(3), "quatre", BigDecimal.valueOf(4), "cinq", BigDecimal.valueOf(5),
        "six", BigDecimal.valueOf(6), "demi", new BigDecimal("0.5"), "demie", new BigDecimal("0.5"));

    /** Alias (sans accents, minuscules) -> unité normalisée. L'ordre compte : plus long d'abord. */
    private static final Map<String, String> UNIT_ALIASES = new LinkedHashMap<>();

    static {
        alias("cas", "c. a soupe", "c.a soupe", "c a soupe", "cuillere a soupe", "cuilleres a soupe",
            "cuil. a soupe", "cs", "c.s.", "cas", "c. a s.", "tbsp");
        alias("cac", "c. a cafe", "c.a cafe", "c a cafe", "cuillere a cafe", "cuilleres a cafe",
            "cuil. a cafe", "cc", "c.c.", "cac", "c. a c.", "tsp");
        alias("kg", "kilogrammes", "kilogramme", "kilos", "kilo", "kg");
        alias("mg", "milligrammes", "milligramme", "mg");
        alias("g", "grammes", "gramme", "gr", "g");
        alias("ml", "millilitres", "millilitre", "ml");
        alias("cl", "centilitres", "centilitre", "cl");
        alias("dl", "decilitres", "decilitre", "dl");
        alias("l", "litres", "litre", "l");
        alias("pincee", "pincees", "pincee");
        alias("sachet", "sachets", "sachet");
        alias("tranche", "tranches", "tranche");
        alias("gousse", "gousses", "gousse");
        alias("botte", "bottes", "botte");
        alias("verre", "verres", "verre");
        alias("boite", "boites", "boite");
        alias("piece", "pieces", "piece", "pc", "pcs", "unites", "unite", "u");
    }

    private static void alias(String unit, String... aliases) {
        for (String a : aliases) {
            UNIT_ALIASES.put(a, unit);
        }
    }

    private QuantityParser() {
    }

    public static ParsedQuantity parse(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return new ParsedQuantity(null, null, null);
        }
        String raw = rawInput.replaceAll("\\s+", " ").trim();
        String text = stripAccents(raw).toLowerCase(Locale.ROOT);

        BigDecimal quantity = null;
        String rest = text;

        Matcher matcher = NUMBER_THEN_UNIT.matcher(text);
        if (matcher.matches()) {
            quantity = new BigDecimal(matcher.group(1).replace(',', '.'));
            if (matcher.group(2) != null) {
                BigDecimal divisor = new BigDecimal(matcher.group(2));
                if (divisor.signum() != 0) {
                    quantity = quantity.divide(divisor, 3, RoundingMode.HALF_UP).stripTrailingZeros();
                }
            }
            rest = matcher.group(3).trim();
        } else {
            String firstWord = text.split(" ", 2)[0];
            if (WORD_NUMBERS.containsKey(firstWord)) {
                quantity = WORD_NUMBERS.get(firstWord);
                rest = text.length() > firstWord.length() ? text.substring(firstWord.length()).trim() : "";
            }
        }

        if (quantity == null) {
            // "selon le goût", "quelques feuilles" : pas de quantité exploitable, le texte brut est conservé
            return new ParsedQuantity(null, null, raw);
        }
        rest = rest.replaceFirst("^(de |d')", "").trim();
        String unit = normalizeUnit(rest);
        if (unit == null) {
            unit = "piece"; // "3" œufs
        }
        return new ParsedQuantity(quantity, unit, raw);
    }

    private static String normalizeUnit(String text) {
        if (text.isEmpty()) {
            return null;
        }
        // On compare le début du texte aux alias : "g de farine" -> g, "c. a soupe rase" -> cas
        String cleaned = text.replaceAll("\\s+", " ");
        String best = null;
        int bestLength = 0;
        for (Map.Entry<String, String> entry : UNIT_ALIASES.entrySet()) {
            String alias = entry.getKey();
            if (alias.length() > bestLength && startsWithWord(cleaned, alias)) {
                best = entry.getValue();
                bestLength = alias.length();
            }
        }
        if (best != null) {
            return best;
        }
        return cleaned.length() > 30 ? cleaned.substring(0, 30) : cleaned;
    }

    private static boolean startsWithWord(String text, String prefix) {
        if (!text.startsWith(prefix)) {
            return false;
        }
        if (text.length() == prefix.length()) {
            return true;
        }
        char next = text.charAt(prefix.length());
        return !Character.isLetterOrDigit(next);
    }

    static String stripAccents(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
            .replace('’', '\'');
    }
}
