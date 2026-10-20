package com.mealplanner.food.ciqual;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.mealplanner.food.ciqual.CiqualValueParser.ParsedValue;

class CiqualValueParserTest {

    @Test
    void parsesFrenchDecimal() {
        ParsedValue value = CiqualValueParser.parse("49,9");
        assertThat(value.value()).isEqualByComparingTo("49.9");
        assertThat(value.qualifier()).isNull();
    }

    @Test
    void parsesLessThanWithLineBreak() {
        ParsedValue value = CiqualValueParser.parse("<\n0,2");
        assertThat(value.value()).isEqualByComparingTo("0.2");
        assertThat(value.qualifier()).isEqualTo("<");
        assertThat(value.raw()).isEqualTo("< 0,2");
        assertThat(value.forCalculation()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void parsesTraces() {
        ParsedValue value = CiqualValueParser.parse("traces");
        assertThat(value.value()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(value.qualifier()).isEqualTo("traces");
    }

    @Test
    void dashMeansMissing() {
        assertThat(CiqualValueParser.parse("-").isEmpty()).isTrue();
        assertThat(CiqualValueParser.parse(null).isEmpty()).isTrue();
    }
}
