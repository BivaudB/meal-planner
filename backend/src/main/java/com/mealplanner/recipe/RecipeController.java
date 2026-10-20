package com.mealplanner.recipe;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mealplanner.common.PageResponse;
import com.mealplanner.recipe.RecipeDtos.RecipeDetail;
import com.mealplanner.recipe.RecipeDtos.RecipeRequest;
import com.mealplanner.recipe.RecipeDtos.RecipeSummary;
import com.mealplanner.user.CurrentUserService;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService service;
    private final CurrentUserService currentUser;

    public RecipeController(RecipeService service, CurrentUserService currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    /** Mes recettes. */
    @GetMapping
    public PageResponse<RecipeSummary> mine(@RequestParam(name = "q", required = false) String query,
                                            @RequestParam(name = "page", defaultValue = "0") int page,
                                            @RequestParam(name = "size", defaultValue = "20") int size) {
        return service.listMine(currentUser.currentUserId(), query, page, size);
    }

    /** Recettes partagées publiquement par tous les utilisateurs. */
    @GetMapping("/public")
    public PageResponse<RecipeSummary> shared(@RequestParam(name = "q", required = false) String query,
                                              @RequestParam(name = "page", defaultValue = "0") int page,
                                              @RequestParam(name = "size", defaultValue = "20") int size) {
        return service.listPublic(currentUser.currentUserId(), query, page, size);
    }

    @GetMapping("/{id}")
    public RecipeDetail get(@PathVariable Long id) {
        return service.get(id, currentUser.currentUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeDetail create(@Validated @RequestBody RecipeRequest request) {
        return service.create(currentUser.currentUserId(), request);
    }

    @PutMapping("/{id}")
    public RecipeDetail update(@PathVariable Long id, @Validated @RequestBody RecipeRequest request) {
        return service.update(id, currentUser.currentUserId(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id, currentUser.currentUserId());
    }
}
