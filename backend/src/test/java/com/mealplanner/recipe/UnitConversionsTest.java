package com.mealplanner.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class UnitConversionsTest {

    @Test
    void convertsKnownUnits() {
        assertThat(UnitConversions.toGrams(new BigDecimal("1.5"), "kg")).isEqualByComparingTo("1500");
        assertThat(UnitConversions.toGrams(new BigDecimal("2"), "cas")).isEqualByComparingTo("30");
        assertThat(UnitConversions.toGrams(new BigDecimal("20"), "cl")).isEqualByComparingTo("200");
    }

    @Test
    void unknownUnitHasNoWeight() {
        assertThat(UnitConversions.toGrams(new BigDecimal("3"), "piece")).isNull();
        assertThat(UnitConversions.toGrams(null, "g")).isNull();
    }
}
