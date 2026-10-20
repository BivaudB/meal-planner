package com.mealplanner.food.ciqual;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.mealplanner.food.ciqual.CiqualValueParser.ParsedValue;

/**
 * Lit le classeur Ciqual publié par l'Anses (Table_Ciqual_AAAA_FR_*.xlsx) :
 * <ul>
 *   <li>feuille 1 "composition nutritionnelle" : une ligne par aliment, une colonne par nutriment ;</li>
 *   <li>feuille 2 "codes INFOODS" : code numérique et libellé de chaque nutriment.</li>
 * </ul>
 * Les en-têtes de la feuille 1 contiennent des retours à la ligne et ont perdu les "/" :
 * on les rapproche des libellés de la feuille 2 après normalisation.
 */
public class CiqualWorkbookReader {

    private static final Pattern UNIT_PATTERN = Pattern.compile("\\(([^()/]+)/100 ?g\\)\\s*$");

    public record NutrientDefinition(int id, String infoodsTag, String name, String unit, int sortOrder) {
    }

    public record FoodGroup(String code, int level, String parentCode, String name) {
    }

    public record FoodRow(long id, String name, String scientificName, String groupCode,
                          Map<Integer, ParsedValue> nutrients) {
    }

    public record CiqualData(List<NutrientDefinition> nutrients, List<FoodGroup> groups, List<FoodRow> foods,
                             List<String> warnings) {
    }

    private final DataFormatter formatter = new DataFormatter(Locale.FRANCE);

    public CiqualData read(InputStream input) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            Sheet compositionSheet = workbook.getSheetAt(0);
            Sheet codesSheet = workbook.getNumberOfSheets() > 1 ? workbook.getSheetAt(1) : null;
            List<String> warnings = new ArrayList<>();

            Map<String, CodeEntry> codesByLabel = codesSheet != null ? readCodes(codesSheet) : Map.of();

            Row header = compositionSheet.getRow(compositionSheet.getFirstRowNum());
            Map<String, Integer> fixedColumns = new HashMap<>();
            Map<Integer, NutrientDefinition> nutrientByColumn = new LinkedHashMap<>();
            int sortOrder = 0;
            for (Cell cell : header) {
                String title = cellText(cell);
                if (title == null) {
                    continue;
                }
                if (title.startsWith("alim_")) {
                    fixedColumns.put(title, cell.getColumnIndex());
                    continue;
                }
                CodeEntry code = codesByLabel.get(normalizeLabel(title));
                if (code == null) {
                    warnings.add("Colonne ignorée (pas de code nutriment correspondant) : " + flatten(title));
                    continue;
                }
                nutrientByColumn.put(cell.getColumnIndex(),
                    new NutrientDefinition(code.id(), code.tag(), nameWithoutUnit(code.label()), unitOf(code.label()),
                        sortOrder++));
            }
            for (String required : List.of("alim_code", "alim_nom_fr")) {
                if (!fixedColumns.containsKey(required)) {
                    throw new IllegalArgumentException("Colonne obligatoire absente du fichier Ciqual : " + required);
                }
            }

            Map<String, FoodGroup> groups = new LinkedHashMap<>();
            List<FoodRow> foods = new ArrayList<>();
            for (int r = header.getRowNum() + 1; r <= compositionSheet.getLastRowNum(); r++) {
                Row row = compositionSheet.getRow(r);
                if (row == null) {
                    continue;
                }
                String code = text(row, fixedColumns.get("alim_code"));
                String name = text(row, fixedColumns.get("alim_nom_fr"));
                if (code == null || name == null) {
                    continue;
                }
                long id;
                try {
                    id = Long.parseLong(code.trim());
                } catch (NumberFormatException e) {
                    warnings.add("Ligne " + (r + 1) + " ignorée : code aliment invalide '" + code + "'");
                    continue;
                }
                String groupCode = registerGroups(row, fixedColumns, groups);
                Map<Integer, ParsedValue> values = new HashMap<>();
                nutrientByColumn.forEach((column, nutrient) -> {
                    ParsedValue value = CiqualValueParser.parse(text(row, column));
                    if (!value.isEmpty()) {
                        values.put(nutrient.id(), value);
                    }
                });
                foods.add(new FoodRow(id, name.trim(), blankToNull(text(row, fixedColumns.get("alim_nom_sci"))),
                    groupCode, values));
            }
            return new CiqualData(List.copyOf(nutrientByColumn.values()), List.copyOf(groups.values()), foods,
                warnings);
        }
    }

    /** Enregistre groupe / sous-groupe / sous-sous-groupe et renvoie le code le plus précis. */
    private String registerGroups(Row row, Map<String, Integer> columns, Map<String, FoodGroup> groups) {
        String[][] levels = {
            {"alim_grp_code", "alim_grp_nom_fr"},
            {"alim_ssgrp_code", "alim_ssgrp_nom_fr"},
            {"alim_ssssgrp_code", "alim_ssssgrp_nom_fr"},
        };
        String parent = null;
        String deepest = null;
        for (int level = 0; level < levels.length; level++) {
            String code = text(row, columns.get(levels[level][0]));
            String name = text(row, columns.get(levels[level][1]));
            if (code == null || name == null || name.isBlank() || name.trim().equals("-") || code.matches("0+")) {
                break;
            }
            code = code.trim();
            groups.putIfAbsent(code, new FoodGroup(code, level + 1, parent, name.trim()));
            parent = code;
            deepest = code;
        }
        return deepest;
    }

    private record CodeEntry(int id, String tag, String label) {
    }

    private Map<String, CodeEntry> readCodes(Sheet sheet) {
        Map<String, CodeEntry> codes = new HashMap<>();
        for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            String tag = text(row, 0);
            String id = text(row, 1);
            String label = text(row, 2);
            if (id == null || label == null) {
                continue;
            }
            try {
                int numericId = Integer.parseInt(id.trim().replaceAll("[.,]0+$", ""));
                codes.put(normalizeLabel(label), new CodeEntry(numericId, blankToNull(tag), flatten(label)));
            } catch (NumberFormatException ignored) {
                // ligne non exploitable
            }
        }
        return codes;
    }

    /** Rend comparables "Eau\n(g\n100\ng)" et "Eau (g/100 g)". */
    static String normalizeLabel(String label) {
        String flat = flatten(label).replace('/', ' ').replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
        return Normalizer.normalize(flat, Normalizer.Form.NFC);
    }

    static String unitOf(String label) {
        Matcher matcher = UNIT_PATTERN.matcher(label);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    static String nameWithoutUnit(String label) {
        return UNIT_PATTERN.matcher(label).replaceFirst("").trim();
    }

    private static String flatten(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    private String text(Row row, Integer column) {
        if (column == null) {
            return null;
        }
        Cell cell = row.getCell(column);
        return cell == null ? null : cellText(cell);
    }

    private String cellText(Cell cell) {
        String value = formatter.formatCellValue(cell);
        return value == null || value.isBlank() ? null : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
