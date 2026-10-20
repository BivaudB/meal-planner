package com.mealplanner.food;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mealplanner.common.NotFoundException;
import com.mealplanner.food.FoodDtos.FoodDetail;
import com.mealplanner.food.FoodDtos.FoodSummary;

@RestController
@RequestMapping("/api/foods")
public class FoodController {

    private final FoodRepository repository;

    public FoodController(FoodRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<FoodSummary> search(@RequestParam("q") String query,
                                    @RequestParam(name = "limit", defaultValue = "20") int limit) {
        if (query.isBlank()) {
            return List.of();
        }
        return repository.search(query.trim(), Math.clamp(limit, 1, 100));
    }

    @GetMapping("/{id}")
    public FoodDetail get(@PathVariable long id) {
        return repository.findDetail(id)
            .orElseThrow(() -> new NotFoundException("Aliment introuvable : " + id));
    }
}
