package com.mealplanner.recipe;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "recipe")
public class Recipe {

    public enum Visibility { PRIVATE, PUBLIC }

    /** CATALOG : recette de base fournie avec l'application (resources/seed/recipes.json). */
    public enum Source { MANUAL, IMPORT, CATALOG, INSTAGRAM, YOUTUBE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    private Integer servings;
    private Integer prepMinutes;
    private Integer cookMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility = Visibility.PRIVATE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source = Source.MANUAL;

    @Column(length = 1000)
    private String sourceUrl;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    // @BatchSize : en liste, les ingrédients de 50 recettes sont chargés en une seule requête
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @BatchSize(size = 50)
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @BatchSize(size = 50)
    private List<RecipeStep> steps = new ArrayList<>();

    protected Recipe() {
    }

    public Recipe(Long ownerId, String name, Source source) {
        this.ownerId = ownerId;
        this.name = name;
        this.source = source;
    }

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public void replaceIngredients(List<RecipeIngredient> newIngredients) {
        ingredients.clear();
        newIngredients.forEach(this::addIngredient);
    }

    public void addIngredient(RecipeIngredient ingredient) {
        ingredient.attachTo(this);
        ingredients.add(ingredient);
    }

    public void replaceSteps(List<String> instructions) {
        steps.clear();
        for (int i = 0; i < instructions.size(); i++) {
            RecipeStep step = new RecipeStep(i + 1, instructions.get(i));
            step.attachTo(this);
            steps.add(step);
        }
    }

    public boolean isOwnedBy(Long userId) {
        return ownerId.equals(userId);
    }

    public Long getId() { return id; }
    public Long getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getServings() { return servings; }
    public void setServings(Integer servings) { this.servings = servings; }
    public Integer getPrepMinutes() { return prepMinutes; }
    public void setPrepMinutes(Integer prepMinutes) { this.prepMinutes = prepMinutes; }
    public Integer getCookMinutes() { return cookMinutes; }
    public void setCookMinutes(Integer cookMinutes) { this.cookMinutes = cookMinutes; }
    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }
    public Source getSource() { return source; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<RecipeIngredient> getIngredients() { return ingredients; }
    public List<RecipeStep> getSteps() { return steps; }
}
