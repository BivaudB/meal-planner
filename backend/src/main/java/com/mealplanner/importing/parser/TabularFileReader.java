package com.mealplanner.importing.parser;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * Lit un fichier tabulaire (.xlsx, .xls, .csv) sous forme de lignes de texte.
 * Pour Excel, seule la première feuille est lue. Pour le CSV, le séparateur
 * (';' ou ',' ou tabulation) et l'encodage (UTF-8 ou Windows-1252) sont détectés.
 */
public final class TabularFileReader {

    private TabularFileReader() {
    }

    public static List<List<String>> read(String filename, byte[] content) throws IOException {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".csv") || lower.endsWith(".txt")) {
            return readCsv(content);
        }
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls") || lower.endsWith(".xlsm")) {
            return readWorkbook(content);
        }
        throw new IllegalArgumentException("Format non supporté : " + filename + " (attendu : .xlsx, .xls ou .csv)");
    }

    private static List<List<String>> readWorkbook(byte[] content) throws IOException {
        DataFormatter formatter = new DataFormatter(Locale.FRANCE);
        List<List<String>> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                List<String> values = new ArrayList<>();
                if (row != null) {
                    for (int c = 0; c < Math.max(0, row.getLastCellNum()); c++) {
                        Cell cell = row.getCell(c);
                        values.add(cell == null ? null : formatter.formatCellValue(cell));
                    }
                }
                rows.add(values);
            }
        }
        return rows;
    }

    private static List<List<String>> readCsv(byte[] content) throws IOException {
        String text = decode(content);
        if (text.startsWith("﻿")) {
            text = text.substring(1); // BOM
        }
        char delimiter = detectDelimiter(text);
        CSVFormat format = CSVFormat.DEFAULT.builder().setDelimiter(delimiter).setIgnoreEmptyLines(false).get();
        List<List<String>> rows = new ArrayList<>();
        try (CSVParser parser = CSVParser.parse(new StringReader(text), format)) {
            for (CSVRecord record : parser) {
                List<String> values = new ArrayList<>();
                record.forEach(values::add);
                rows.add(values);
            }
        }
        return rows;
    }

    private static String decode(byte[] content) throws IOException {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .decode(java.nio.ByteBuffer.wrap(content))
                .toString();
        } catch (CharacterCodingException notUtf8) {
            // Fichiers CSV enregistrés par Excel sous Windows
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(content), java.nio.charset.Charset.forName("windows-1252")))) {
                StringBuilder builder = new StringBuilder();
                reader.lines().forEach(line -> builder.append(line).append('\n'));
                return builder.toString();
            }
        }
    }

    private static char detectDelimiter(String text) {
        String firstLine = text.lines().filter(line -> !line.isBlank()).findFirst().orElse("");
        long semicolons = firstLine.chars().filter(ch -> ch == ';').count();
        long commas = firstLine.chars().filter(ch -> ch == ',').count();
        long tabs = firstLine.chars().filter(ch -> ch == '\t').count();
        if (tabs > semicolons && tabs > commas) {
            return '\t';
        }
        return semicolons >= commas && semicolons > 0 ? ';' : ',';
    }
}
