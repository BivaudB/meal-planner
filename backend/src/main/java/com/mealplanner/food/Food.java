package com.mealplanner.food;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Aliment du référentiel Ciqual ; l'identifiant est le code alim_code. Valeurs clés pour 100 g,
 * dénormalisées depuis food_nutrient. En lecture seule : la table est alimentée par l'import
 * Ciqual (insertions par lots en SQL, voir CiqualImportService).
 */
@Entity
@Immutable
@Table(name = "food")
public class Food {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "scientific_name")
    private String scientificName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_code")
    private FoodGroup group;

    @Column(name = "energy_kcal")
    private BigDecimal energyKcal;

    @Column(name = "energy_kj")
    private BigDecimal energyKj;

    @Column(name = "protein_g")
    private BigDecimal proteinG;

    @Column(name = "carbohydrate_g")
    private BigDecimal carbohydrateG;

    @Column(name = "sugars_g")
    private BigDecimal sugarsG;

    @Column(name = "fat_g")
    private BigDecimal fatG;

    @Column(name = "saturated_fat_g")
    private BigDecimal saturatedFatG;

    @Column(name = "fiber_g")
    private BigDecimal fiberG;

    @Column(name = "salt_g")
    private BigDecimal saltG;

    @Column(nullable = false)
    private String source;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Food() {
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getScientificName() { return scientificName; }
    public FoodGroup getGroup() { return group; }
    public BigDecimal getEnergyKcal() { return energyKcal; }
    public BigDecimal getEnergyKj() { return energyKj; }
    public BigDecimal getProteinG() { return proteinG; }
    public BigDecimal getCarbohydrateG() { return carbohydrateG; }
    public BigDecimal getSugarsG() { return sugarsG; }
    public BigDecimal getFatG() { return fatG; }
    public BigDecimal getSaturatedFatG() { return saturatedFatG; }
    public BigDecimal getFiberG() { return fiberG; }
    public BigDecimal getSaltG() { return saltG; }
    public String getSource() { return source; }
    public Instant getUpdatedAt() { return updatedAt; }
}
