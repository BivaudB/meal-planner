package com.mealplanner.food;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mealplanner.common.PageResponse;
import com.mealplanner.food.FoodDtos.CatalogSort;
import com.mealplanner.food.FoodDtos.FoodCatalogItem;
import com.mealplanner.food.FoodDtos.FoodCategory;
import com.mealplanner.food.FoodDtos.FoodDetail;
import com.mealplanner.food.FoodDtos.FoodSubcategory;
import com.mealplanner.food.FoodDtos.FoodSummary;
import com.mealplanner.food.FoodDtos.NutrientValue;
import com.mealplanner.food.FoodRepository.GroupCount;
import com.mealplanner.food.FoodRepository.ScoredFood;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.criteria.Predicate;

/** Lecture du référentiel Ciqual : les entités sont converties en DTO avant de sortir du service. */
@Service
@Transactional(readOnly = true)
public class FoodService {

    private final FoodRepository foods;
    private final FoodGroupRepository groups;
    private final FoodNutrientRepository nutrients;

    public FoodService(FoodRepository foods, FoodGroupRepository groups, FoodNutrientRepository nutrients) {
        this.foods = foods;
        this.groups = groups;
        this.nutrients = nutrients;
    }

    public List<FoodSummary> search(String query, int limit) {
        return foods.search(query, limit).stream().map(FoodService::toSummary).toList();
    }

    /** Meilleur aliment correspondant à un nom d'ingrédient, si son score atteint le seuil. */
    public Optional<FoodSummary> bestMatch(String ingredientName, double threshold) {
        return search(ingredientName, 1).stream()
            .filter(food -> food.score() != null && food.score() >= threshold)
            .findFirst();
    }

    public Optional<FoodDetail> findDetail(long id) {
        return foods.findWithGroupById(id).map(food -> new FoodDetail(
            food.getId(),
            food.getName(),
            food.getScientificName(),
            groupName(food),
            food.getEnergyKcal(),
            food.getEnergyKj(),
            food.getProteinG(),
            food.getCarbohydrateG(),
            food.getSugarsG(),
            food.getFatG(),
            food.getSaturatedFatG(),
            food.getFiberG(),
            food.getSaltG(),
            nutrients.findByFoodId(id).stream()
                .map(fn -> new NutrientValue(fn.getNutrient().getId(), fn.getNutrient().getName(),
                    fn.getNutrient().getUnit(), fn.getValue(), fn.getQualifier(), fn.getRawValue()))
                .toList()));
    }

    /** Valeurs clés (pour 100 g) d'un ensemble d'aliments, indexées par identifiant. */
    public Map<Long, FoodSummary> findSummaries(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap(); // (contrairement à Map.of(), get(null) renvoie null)
        }
        return foods.findWithGroupByIdIn(ids).stream()
            .map(food -> new FoodSummary(food.getId(), food.getName(), groupName(food), food.getEnergyKcal(),
                food.getProteinG(), food.getCarbohydrateG(), food.getFatG(), null))
            .collect(Collectors.toMap(FoodSummary::id, Function.identity()));
    }

    /**
     * Catalogue paginé, filtrable par texte et par groupe / sous-groupe ({@code groupCode} :
     * 2 ou 4 chiffres, les codes Ciqual étant hiérarchiques), trié sur une valeur pour 100 g.
     * Les aliments sans valeur pour le critère de tri passent en dernier.
     */
    public PageResponse<FoodCatalogItem> catalog(String query, String groupCode, CatalogSort sort,
                                                 boolean descending, int page, int size) {
        Specification<Food> spec = (root, criteria, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query != null && !query.isBlank()) {
                // value() : paramètre lié (literal() serait recopié tel quel dans le SQL)
                Expression<String> text = ((HibernateCriteriaBuilder) cb).value(query.trim());
                predicates.add(cb.like(normalize(cb, root.get("name")),
                    cb.concat(cb.concat("%", normalize(cb, text)), "%")));
            }
            if (groupCode != null && !groupCode.isBlank()) {
                predicates.add(cb.like(root.<FoodGroup>get("group").<String>get("code"), groupCode.trim() + "%"));
            }
            // Tri porté par la spécification (la requête de comptage n'en a pas besoin)
            if (criteria != null && criteria.getResultType() != Long.class) {
                Expression<String> name = normalize(cb, root.get("name"));
                Expression<?> column = sort == CatalogSort.NAME ? name : root.get(sortAttribute(sort));
                criteria.orderBy(
                    descending ? cb.desc(column, Nulls.LAST) : cb.asc(column, Nulls.LAST),
                    cb.asc(name),
                    cb.asc(root.get("id")));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Map<String, FoodGroup> groupsByCode = groupsByCode();
        return PageResponse.of(foods.findAll(spec, PageRequest.of(page, size)), food -> {
            String code = food.getGroup() == null ? null : food.getGroup().getCode();
            FoodGroup category = code == null ? null : groupsByCode.get(code.substring(0, 2));
            FoodGroup subcategory = code == null || code.length() < 4 ? null : groupsByCode.get(code.substring(0, 4));
            return new FoodCatalogItem(
                food.getId(),
                food.getName(),
                category == null ? null : category.getCode(),
                category == null ? null : category.getName(),
                subcategory == null ? null : subcategory.getCode(),
                subcategory == null ? null : subcategory.getName(),
                food.getEnergyKcal(),
                food.getProteinG(),
                food.getCarbohydrateG(),
                food.getFatG());
        });
    }

    /** Groupes et sous-groupes Ciqual qui contiennent au moins un aliment. */
    public List<FoodCategory> categories() {
        Map<String, FoodGroup> groupsByCode = groupsByCode();
        // code de groupe -> (code de sous-groupe, ou "" pour les aliments rattachés au groupe seul) -> nombre
        Map<String, Map<String, Long>> counts = new TreeMap<>();
        for (GroupCount row : foods.countByGroup()) {
            String code = row.getGroupCode();
            if (code.length() < 2 || !groupsByCode.containsKey(code.substring(0, 2))) {
                continue;
            }
            String subCode = code.length() >= 4 && groupsByCode.containsKey(code.substring(0, 4))
                ? code.substring(0, 4) : "";
            counts.computeIfAbsent(code.substring(0, 2), k -> new TreeMap<>())
                .merge(subCode, row.getFoodCount(), Long::sum);
        }
        return counts.entrySet().stream()
            .map(entry -> new FoodCategory(
                entry.getKey(),
                groupsByCode.get(entry.getKey()).getName(),
                entry.getValue().values().stream().mapToLong(Long::longValue).sum(),
                entry.getValue().entrySet().stream()
                    .filter(sub -> !sub.getKey().isEmpty())
                    .map(sub -> new FoodSubcategory(sub.getKey(), groupsByCode.get(sub.getKey()).getName(),
                        sub.getValue()))
                    .toList()))
            .toList();
    }

    public boolean exists(long id) {
        return foods.existsById(id);
    }

    public long count() {
        return foods.count();
    }

    private Map<String, FoodGroup> groupsByCode() {
        return groups.findAll().stream().collect(Collectors.toMap(FoodGroup::getCode, Function.identity()));
    }

    private static String sortAttribute(CatalogSort sort) {
        return switch (sort) {
            case NAME -> "name";
            case ENERGY_KCAL -> "energyKcal";
            case PROTEIN_G -> "proteinG";
            case CARBOHYDRATE_G -> "carbohydrateG";
            case FAT_G -> "fatG";
        };
    }

    /** f_normalize() (migration V1) : minuscules sans accents, comme l'index de recherche. */
    private static Expression<String> normalize(CriteriaBuilder cb, Expression<?> text) {
        return cb.function("f_normalize", String.class, text);
    }

    private static String groupName(Food food) {
        return food.getGroup() == null ? null : food.getGroup().getName();
    }

    private static FoodSummary toSummary(ScoredFood food) {
        return new FoodSummary(food.getId(), food.getName(), food.getGroupName(), food.getEnergyKcal(),
            food.getProteinG(), food.getCarbohydrateG(), food.getFatG(), food.getScore());
    }
}
