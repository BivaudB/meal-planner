package com.mealplanner.recipe;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "recipe_ingredient")
public class RecipeIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private String name;

    @Column(precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(length = 30)
    private String unit;

    @Column(length = 100)
    private String rawQuantity;

    /** Aliment Ciqual rattaché (null si aucune correspondance fiable). */
    private Long foodId;

    private Double matchScore;

    protected RecipeIngredient() {
    }

    public RecipeIngredient(int position, String name, BigDecimal quantity, String unit, String rawQuantity) {
        this.position = position;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.rawQuantity = rawQuantity;
    }

    void attachTo(Recipe recipe) {
        this.recipe = recipe;
    }

    public void linkFood(Long foodId, Double matchScore) {
        this.foodId = foodId;
        this.matchScore = matchScore;
    }

    public Long getId() { return id; }
    public int getPosition() { return position; }
    public String getName() { return name; }
    public BigDecimal getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public String getRawQuantity() { return rawQuantity; }
    public Long getFoodId() { return foodId; }
    public Double getMatchScore() { return matchScore; }
}
