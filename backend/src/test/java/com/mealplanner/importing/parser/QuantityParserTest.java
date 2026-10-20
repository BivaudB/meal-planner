package com.mealplanner.importing.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import com.mealplanner.importing.parser.QuantityParser.ParsedQuantity;

class QuantityParserTest {

    @ParameterizedTest(name = "« {0} » -> {1} {2}")
    @CsvSource(delimiter = '|', value = {
        "200 g          | 200   | g",
        "200g           | 200   | g",
        "1,5 kg         | 1.5   | kg",
        "20 cl          | 20    | cl",
        "2 c. à soupe   | 2     | cas",
        "1 cuillère à café | 1  | cac",
        "1 pincée       | 1     | pincee",
        "3              | 3     | piece",
        "1/2            | 0.5   | piece",
        "une gousse     | 1     | gousse",
        "250 ml de lait | 250   | ml",
        "2 tranches     | 2     | tranche",
    })
    void parsesQuantityAndUnit(String input, String expectedQuantity, String expectedUnit) {
        ParsedQuantity parsed = QuantityParser.parse(input);
        assertThat(parsed.quantity()).isEqualByComparingTo(new BigDecimal(expectedQuantity));
        assertThat(parsed.unit()).isEqualTo(expectedUnit);
        assertThat(parsed.raw()).isEqualTo(input.trim());
    }

    @Test
    void keepsTextWhenNoNumber() {
        ParsedQuantity parsed = QuantityParser.parse("selon le goût");
        assertThat(parsed.quantity()).isNull();
        assertThat(parsed.unit()).isNull();
        assertThat(parsed.raw()).isEqualTo("selon le goût");
    }

    @Test
    void emptyInput() {
        assertThat(QuantityParser.parse("  ").quantity()).isNull();
        assertThat(QuantityParser.parse(null).raw()).isNull();
    }
}
