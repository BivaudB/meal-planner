package com.mealplanner.food;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.mealplanner.food.FoodDtos.CatalogSort;

class FoodControllerTest {

    @Test
    void parsesCamelCaseSortNames() {
        assertThat(FoodController.parseSort("name")).isEqualTo(CatalogSort.NAME);
        assertThat(FoodController.parseSort("energyKcal")).isEqualTo(CatalogSort.ENERGY_KCAL);
        assertThat(FoodController.parseSort("carbohydrateG")).isEqualTo(CatalogSort.CARBOHYDRATE_G);
    }

    @Test
    void rejectsUnknownSort() {
        assertThatThrownBy(() -> FoodController.parseSort("f.name; DROP TABLE food"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
