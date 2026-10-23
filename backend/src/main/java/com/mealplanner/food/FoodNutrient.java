package com.mealplanner.food;

import java.io.Serializable;
import java.math.BigDecimal;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Valeur d'un nutriment pour 100 g d'aliment. {@code qualifier} vaut "<" ou "traces" le cas échéant.
 * En lecture seule : alimenté par l'import Ciqual.
 */
@Entity
@Immutable
@Table(name = "food_nutrient")
public class FoodNutrient {

    @EmbeddedId
    private Key id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nutrient_id", insertable = false, updatable = false)
    private Nutrient nutrient;

    private BigDecimal value;

    private String qualifier;

    @Column(name = "raw_value")
    private String rawValue;

    protected FoodNutrient() {
    }

    public Long getFoodId() { return id.foodId; }
    public Nutrient getNutrient() { return nutrient; }
    public BigDecimal getValue() { return value; }
    public String getQualifier() { return qualifier; }
    public String getRawValue() { return rawValue; }

    @Embeddable
    public record Key(@Column(name = "food_id") Long foodId, @Column(name = "nutrient_id") Integer nutrientId)
        implements Serializable {
    }
}
