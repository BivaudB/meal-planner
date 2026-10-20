package com.mealplanner.pantry;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mealplanner.common.NotFoundException;
import com.mealplanner.food.FoodRepository;
import com.mealplanner.recipe.IngredientMatcher;
import com.mealplanner.user.CurrentUserService;

/** Ingrédients disponibles de l'utilisateur connecté. */
@RestController
@RequestMapping("/api/pantry")
public class PantryController {

    private final PantryItemRepository items;
    private final FoodRepository foods;
    private final IngredientMatcher matcher;
    private final CurrentUserService currentUser;

    public PantryController(PantryItemRepository items, FoodRepository foods, IngredientMatcher matcher,
                            CurrentUserService currentUser) {
        this.items = items;
        this.foods = foods;
        this.matcher = matcher;
        this.currentUser = currentUser;
    }

    public record PantryItemRequest(@NotBlank @Size(max = 255) String name, Long foodId, BigDecimal quantity,
                                    @Size(max = 30) String unit) {
    }

    public record PantryItemResponse(Long id, String name, Long foodId, BigDecimal quantity, String unit) {

        static PantryItemResponse from(PantryItem item) {
            return new PantryItemResponse(item.getId(), item.getName(), item.getFoodId(), item.getQuantity(),
                item.getUnit());
        }
    }

    @GetMapping
    public List<PantryItemResponse> list() {
        return items.findByUserIdOrderByNameAsc(currentUser.currentUserId()).stream()
            .map(PantryItemResponse::from)
            .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PantryItemResponse add(@Validated @RequestBody PantryItemRequest request) {
        Long foodId = request.foodId();
        if (foodId != null && !foods.exists(foodId)) {
            throw new IllegalArgumentException("Aliment inconnu : " + foodId);
        }
        if (foodId == null) {
            foodId = matcher.match(request.name()).map(food -> food.id()).orElse(null);
        }
        PantryItem item = new PantryItem(currentUser.currentUserId(), request.name().trim(), foodId,
            request.quantity(), request.unit());
        return PantryItemResponse.from(items.save(item));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id) {
        PantryItem item = items.findByIdAndUserId(id, currentUser.currentUserId())
            .orElseThrow(() -> new NotFoundException("Ingrédient introuvable : " + id));
        items.delete(item);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void clear() {
        items.deleteByUserId(currentUser.currentUserId());
    }
}
