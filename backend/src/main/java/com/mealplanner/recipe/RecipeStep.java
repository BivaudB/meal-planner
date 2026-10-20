package com.mealplanner.recipe;

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
@Table(name = "recipe_step")
public class RecipeStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, columnDefinition = "text")
    private String instruction;

    protected RecipeStep() {
    }

    RecipeStep(int position, String instruction) {
        this.position = position;
        this.instruction = instruction;
    }

    void attachTo(Recipe recipe) {
        this.recipe = recipe;
    }

    public Long getId() { return id; }
    public int getPosition() { return position; }
    public String getInstruction() { return instruction; }
}
