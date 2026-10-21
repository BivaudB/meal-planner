package com.mealplanner.food;

import java.util.List;
import java.util.Locale;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mealplanner.common.NotFoundException;
import com.mealplanner.common.PageResponse;
import com.mealplanner.food.FoodDtos.CatalogSort;
import com.mealplanner.food.FoodDtos.FoodCatalogItem;
import com.mealplanner.food.FoodDtos.FoodCategory;
import com.mealplanner.food.FoodDtos.FoodDetail;
import com.mealplanner.food.FoodDtos.FoodSummary;

@RestController
@RequestMapping("/api/foods")
public class FoodController {

    private final FoodRepository repository;

    public FoodController(FoodRepository repository) {
        this.repository = repository;
    }

    /** Recherche approximative. {@code basic=true} : ingrédients de base uniquement (pas de plats). */
    @GetMapping
    public List<FoodSummary> search(@RequestParam("q") String query,
                                    @RequestParam(name = "limit", defaultValue = "20") int limit,
                                    @RequestParam(name = "basic", defaultValue = "false") boolean basic) {
        if (query.isBlank()) {
            return List.of();
        }
        return repository.search(query.trim(), Math.clamp(limit, 1, 100), basic);
    }

    /**
     * Catalogue paginé. {@code group} : code de groupe (2 chiffres) ou de sous-groupe (4 chiffres) ;
     * {@code sort} : name, energyKcal, proteinG, carbohydrateG ou fatG ; {@code direction} : asc ou desc.
     */
    @GetMapping("/catalog")
    public PageResponse<FoodCatalogItem> catalog(@RequestParam(name = "q", required = false) String query,
                                                 @RequestParam(name = "group", required = false) String group,
                                                 @RequestParam(name = "basic", defaultValue = "false") boolean basic,
                                                 @RequestParam(name = "sort", defaultValue = "name") String sort,
                                                 @RequestParam(name = "direction", defaultValue = "asc") String direction,
                                                 @RequestParam(name = "page", defaultValue = "0") int page,
                                                 @RequestParam(name = "size", defaultValue = "50") int size) {
        if (group != null && !group.isBlank() && !group.matches("\\d{2}|\\d{4}")) {
            throw new IllegalArgumentException("Code de catégorie invalide : " + group);
        }
        return repository.catalog(query, group, basic, parseSort(sort), "desc".equalsIgnoreCase(direction),
            Math.max(page, 0), Math.clamp(size, 1, 200));
    }

    @GetMapping("/categories")
    public List<FoodCategory> categories(@RequestParam(name = "basic", defaultValue = "false") boolean basic) {
        return repository.categories(basic);
    }

    @GetMapping("/{id}")
    public FoodDetail get(@PathVariable long id) {
        return repository.findDetail(id)
            .orElseThrow(() -> new NotFoundException("Aliment introuvable : " + id));
    }

    /** "proteinG" → PROTEIN_G. */
    static CatalogSort parseSort(String sort) {
        String constant = sort.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
        try {
            return CatalogSort.valueOf(constant);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Critère de tri inconnu : " + sort);
        }
    }
}
