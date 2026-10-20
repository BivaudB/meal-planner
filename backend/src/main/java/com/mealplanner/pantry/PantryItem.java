package com.mealplanner.pantry;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Ingrédient dont l'utilisateur dispose (son "garde-manger"). */
@Entity
@Table(name = "pantry_item")
public class PantryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    private Long foodId;

    @Column(precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(length = 30)
    private String unit;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected PantryItem() {
    }

    public PantryItem(Long userId, String name, Long foodId, BigDecimal quantity, String unit) {
        this.userId = userId;
        this.name = name;
        this.foodId = foodId;
        this.quantity = quantity;
        this.unit = unit;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public Long getFoodId() { return foodId; }
    public BigDecimal getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public Instant getCreatedAt() { return createdAt; }
}
