package com.mealplanner.food;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FoodNutrientRepository extends JpaRepository<FoodNutrient, FoodNutrient.Key> {

    /** Composition d'un aliment, dans l'ordre des colonnes du fichier Ciqual. */
    @Query("""
        SELECT fn FROM FoodNutrient fn JOIN FETCH fn.nutrient n
        WHERE fn.id.foodId = :foodId
        ORDER BY n.sortOrder
        """)
    List<FoodNutrient> findByFoodId(@Param("foodId") long foodId);
}
