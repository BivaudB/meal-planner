package com.mealplanner.food;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Groupe Ciqual. Codes hiérarchiques : 02 (groupe), 0201 (sous-groupe), 020101 (sous-sous-groupe).
 * En lecture seule : la table est alimentée par l'import Ciqual.
 */
@Entity
@Immutable
@Table(name = "food_group")
public class FoodGroup {

    @Id
    private String code;

    @Column(nullable = false)
    private short level;

    @Column(name = "parent_code")
    private String parentCode;

    @Column(nullable = false)
    private String name;

    protected FoodGroup() {
    }

    public String getCode() { return code; }
    public short getLevel() { return level; }
    public String getParentCode() { return parentCode; }
    public String getName() { return name; }
}
