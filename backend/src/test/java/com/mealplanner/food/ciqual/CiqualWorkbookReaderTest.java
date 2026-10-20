package com.mealplanner.food.ciqual;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.mealplanner.food.ciqual.CiqualWorkbookReader.CiqualData;
import com.mealplanner.food.ciqual.CiqualWorkbookReader.FoodRow;

/** Reproduit en miniature la structure du fichier Table_Ciqual_2025_FR. */
class CiqualWorkbookReaderTest {

    @Test
    void readsFoodsGroupsAndNutrients() throws IOException {
        CiqualData data = new CiqualWorkbookReader().read(new ByteArrayInputStream(sampleWorkbook()));

        assertThat(data.nutrients()).hasSize(2);
        assertThat(data.nutrients().getFirst().id()).isEqualTo(328);
        assertThat(data.nutrients().getFirst().unit()).isEqualTo("kcal");
        assertThat(data.nutrients().getFirst().name()).isEqualTo("Energie, Règlement UE N° 1169/2011");

        assertThat(data.groups()).extracting(g -> g.code()).containsExactly("01", "0101");
        assertThat(data.groups().get(1).parentCode()).isEqualTo("01");

        assertThat(data.foods()).hasSize(2);
        FoodRow salad = data.foods().get(1);
        assertThat(salad.id()).isEqualTo(25601L);
        assertThat(salad.groupCode()).isEqualTo("0101");
        assertThat(salad.nutrients().get(328).value()).isEqualByComparingTo("117");
        assertThat(salad.nutrients().get(32000).qualifier()).isEqualTo("<");

        FoodRow average = data.foods().getFirst();
        assertThat(average.groupCode()).isNull();

        assertThat(data.warnings()).singleElement().asString().contains("Facteur de Jones");
    }

    private static byte[] sampleWorkbook() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet composition = workbook.createSheet("composition nutritionnelle");
            fill(composition.createRow(0), "alim_grp_code", "alim_ssgrp_code", "alim_ssssgrp_code", "alim_grp_nom_fr",
                "alim_ssgrp_nom_fr", "alim_ssssgrp_nom_fr", "alim_code", "alim_nom_fr", "alim_nom_sci",
                "Energie,\nRèglement\nUE N°\n1169\n2011 (kcal\n100 g)", "Sucres\n(g\n100\ng)", "Facteur de Jones");
            fill(composition.createRow(1), "00", "0000", "000000", null, null, null, "24999",
                "Dessert (aliment moyen)", "", "255", "21,3", "6,25");
            fill(composition.createRow(2), "01", "0101", "000000", "entrées et plats composés",
                "salades composées et crudités", "-", "25601", "Salade de thon et légumes, appertisée", "",
                "117", "<\n0,2", "6,25");

            Sheet codes = workbook.createSheet("codes INFOODS");
            fill(codes.createRow(0), "INFDSTAG", "ORIGCPCD", "const_nom_fr");
            Row kcal = codes.createRow(1);
            kcal.createCell(0).setCellValue("ENERC");
            kcal.createCell(1).setCellValue(328);
            kcal.createCell(2).setCellValue("Energie, Règlement UE N° 1169/2011 (kcal/100 g)");
            Row sugar = codes.createRow(2);
            sugar.createCell(0).setCellValue("SUGAR");
            sugar.createCell(1).setCellValue(32000);
            sugar.createCell(2).setCellValue("Sucres (g/100 g)");

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static void fill(Row row, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                row.createCell(i).setCellValue(values[i]);
            }
        }
    }
}
