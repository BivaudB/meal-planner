package com.mealplanner.food;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Constituant Ciqual (code ORIGCPCD). En lecture seule : alimenté par l'import Ciqual. */
@Entity
@Immutable
@Table(name = "nutrient")
public class Nutrient {

    @Id
    private Integer id;

    @Column(name = "infoods_tag")
    private String infoodsTag;

    @Column(nullable = false)
    private String name;

    private String unit;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Nutrient() {
    }

    public Integer getId() { return id; }
    public String getInfoodsTag() { return infoodsTag; }
    public String getName() { return name; }
    public String getUnit() { return unit; }
    public int getSortOrder() { return sortOrder; }
}
