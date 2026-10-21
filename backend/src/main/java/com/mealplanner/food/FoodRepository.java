package com.mealplanner.food;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.mealplanner.common.PageResponse;
import com.mealplanner.food.FoodDtos.CatalogSort;
import com.mealplanner.food.FoodDtos.FoodCatalogItem;
import com.mealplanner.food.FoodDtos.FoodCategory;
import com.mealplanner.food.FoodDtos.FoodSubcategory;
import com.mealplanner.food.FoodDtos.FoodDetail;
import com.mealplanner.food.FoodDtos.FoodSummary;
import com.mealplanner.food.FoodDtos.NutrientValue;

/**
 * Accès en lecture au référentiel Ciqual. En SQL direct (JdbcClient) car les requêtes
 * s'appuient sur la recherche approximative de PostgreSQL (pg_trgm).
 */
@Repository
public class FoodRepository {

    /**
     * Recherche approximative, insensible à la casse et aux accents.
     * word_similarity mesure si le texte recherché apparaît (même partiellement) dans le nom.
     * Départage : noms qui commencent par la recherche, puis noms les plus courts
     * ("Tomate, crue" passe avant "Sauce tomate aux oignons, préemballée").
     */
    private static final String SEARCH_SQL = """
        SELECT f.id, f.name, g.name AS group_name, f.energy_kcal, f.protein_g, f.carbohydrate_g, f.fat_g,
               word_similarity(f_normalize(:q), f_normalize(f.name)) AS score
        FROM food f
        LEFT JOIN food_group g ON g.code = f.group_code
        WHERE (f_normalize(f.name) %> f_normalize(:q)
           OR f_normalize(f.name) LIKE '%' || f_normalize(:q) || '%')
          /*filter*/
        ORDER BY score DESC,
                 (f_normalize(f.name) LIKE f_normalize(:q) || '%') DESC,
                 length(f.name) ASC
        LIMIT :limit
        """;

    private final JdbcClient jdbc;

    public FoodRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<FoodSummary> search(String query, int limit) {
        return search(query, limit, false);
    }

    /** @param basicOnly limite aux ingrédients de base (voir {@link BasicIngredients}) */
    public List<FoodSummary> search(String query, int limit, boolean basicOnly) {
        return jdbc.sql(SEARCH_SQL.replace("/*filter*/", basicOnly ? "AND " + BasicIngredients.SQL_PREDICATE : ""))
            .param("q", query)
            .param("limit", limit)
            .query((rs, i) -> new FoodSummary(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("group_name"),
                rs.getBigDecimal("energy_kcal"),
                rs.getBigDecimal("protein_g"),
                rs.getBigDecimal("carbohydrate_g"),
                rs.getBigDecimal("fat_g"),
                rs.getDouble("score")))
            .list();
    }

    /** Meilleur aliment correspondant à un nom d'ingrédient, si son score atteint le seuil. */
    public Optional<FoodSummary> bestMatch(String ingredientName, double threshold) {
        return search(ingredientName, 1).stream()
            .filter(food -> food.score() != null && food.score() >= threshold)
            .findFirst();
    }

    public Optional<FoodDetail> findDetail(long id) {
        List<NutrientValue> nutrients = jdbc.sql("""
                SELECT n.id, n.name, n.unit, fn.value, fn.qualifier, fn.raw_value
                FROM food_nutrient fn JOIN nutrient n ON n.id = fn.nutrient_id
                WHERE fn.food_id = :id
                ORDER BY n.sort_order
                """)
            .param("id", id)
            .query((rs, i) -> new NutrientValue(
                rs.getInt("id"), rs.getString("name"), rs.getString("unit"),
                rs.getBigDecimal("value"), rs.getString("qualifier"), rs.getString("raw_value")))
            .list();

        return jdbc.sql("""
                SELECT f.*, g.name AS group_name
                FROM food f LEFT JOIN food_group g ON g.code = f.group_code
                WHERE f.id = :id
                """)
            .param("id", id)
            .query((rs, i) -> new FoodDetail(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("scientific_name"),
                rs.getString("group_name"),
                rs.getBigDecimal("energy_kcal"),
                rs.getBigDecimal("energy_kj"),
                rs.getBigDecimal("protein_g"),
                rs.getBigDecimal("carbohydrate_g"),
                rs.getBigDecimal("sugars_g"),
                rs.getBigDecimal("fat_g"),
                rs.getBigDecimal("saturated_fat_g"),
                rs.getBigDecimal("fiber_g"),
                rs.getBigDecimal("salt_g"),
                nutrients))
            .optional();
    }

    /** Valeurs clés (pour 100 g) d'un ensemble d'aliments, indexées par identifiant. */
    public Map<Long, FoodSummary> findSummaries(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap(); // (contrairement à Map.of(), get(null) renvoie null)
        }
        return jdbc.sql("""
                SELECT f.id, f.name, g.name AS group_name, f.energy_kcal, f.protein_g, f.carbohydrate_g, f.fat_g
                FROM food f LEFT JOIN food_group g ON g.code = f.group_code
                WHERE f.id IN (:ids)
                """)
            .param("ids", ids)
            .query((rs, i) -> new FoodSummary(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("group_name"),
                rs.getBigDecimal("energy_kcal"),
                rs.getBigDecimal("protein_g"),
                rs.getBigDecimal("carbohydrate_g"),
                rs.getBigDecimal("fat_g"),
                null))
            .list().stream()
            .collect(Collectors.toMap(FoodSummary::id, Function.identity()));
    }

    /**
     * Catalogue paginé, filtrable par texte et par groupe / sous-groupe ({@code groupCode} :
     * 2 ou 4 chiffres, les codes Ciqual étant hiérarchiques), trié sur une valeur pour 100 g.
     * Les aliments sans valeur pour le critère de tri passent en dernier.
     */
    public PageResponse<FoodCatalogItem> catalog(String query, String groupCode, boolean basicOnly,
                                                 CatalogSort sort, boolean descending, int page, int size) {
        StringBuilder where = new StringBuilder("WHERE TRUE");
        Map<String, Object> params = new HashMap<>();
        if (query != null && !query.isBlank()) {
            where.append(" AND f_normalize(f.name) LIKE '%' || f_normalize(:q) || '%'");
            params.put("q", query.trim());
        }
        if (groupCode != null && !groupCode.isBlank()) {
            where.append(" AND f.group_code LIKE :group || '%'");
            params.put("group", groupCode.trim());
        }
        if (basicOnly) {
            where.append(" AND ").append(BasicIngredients.SQL_PREDICATE);
        }

        long total = jdbc.sql("SELECT count(*) FROM food f " + where)
            .params(params)
            .query(Long.class).single();

        String column = switch (sort) {
            case NAME -> "f_normalize(f.name)";
            case ENERGY_KCAL -> "f.energy_kcal";
            case PROTEIN_G -> "f.protein_g";
            case CARBOHYDRATE_G -> "f.carbohydrate_g";
            case FAT_G -> "f.fat_g";
        };
        String direction = descending ? "DESC" : "ASC";
        List<FoodCatalogItem> content = jdbc.sql("""
                SELECT f.id, f.name, c.code AS category_code, c.name AS category_name,
                       s.code AS subcategory_code, s.name AS subcategory_name,
                       f.energy_kcal, f.protein_g, f.carbohydrate_g, f.fat_g
                FROM food f
                LEFT JOIN food_group c ON c.code = left(f.group_code, 2)
                LEFT JOIN food_group s ON s.code = left(f.group_code, 4) AND length(f.group_code) >= 4
                """ + where + " ORDER BY " + column + " " + direction + " NULLS LAST, f_normalize(f.name), f.id"
                + " LIMIT :limit OFFSET :offset")
            .params(params)
            .param("limit", size)
            .param("offset", (long) page * size)
            .query((rs, i) -> new FoodCatalogItem(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("category_code"),
                rs.getString("category_name"),
                rs.getString("subcategory_code"),
                rs.getString("subcategory_name"),
                rs.getBigDecimal("energy_kcal"),
                rs.getBigDecimal("protein_g"),
                rs.getBigDecimal("carbohydrate_g"),
                rs.getBigDecimal("fat_g")))
            .list();
        int totalPages = (int) ((total + size - 1) / size);
        return new PageResponse<>(content, page, size, total, totalPages);
    }

    /** Groupes et sous-groupes Ciqual qui contiennent au moins un aliment (du périmètre demandé). */
    public List<FoodCategory> categories(boolean basicOnly) {
        record Row(String code, String name, String subCode, String subName, long count) {
        }
        List<Row> rows = jdbc.sql("""
                SELECT c.code, c.name, s.code AS sub_code, s.name AS sub_name, count(*) AS food_count
                FROM food f
                JOIN food_group c ON c.code = left(f.group_code, 2)
                LEFT JOIN food_group s ON s.code = left(f.group_code, 4) AND length(f.group_code) >= 4
                """ + (basicOnly ? "WHERE " + BasicIngredients.SQL_PREDICATE : "") + """

                GROUP BY c.code, c.name, s.code, s.name
                ORDER BY c.code, s.code NULLS FIRST
                """)
            .query((rs, i) -> new Row(rs.getString("code"), rs.getString("name"), rs.getString("sub_code"),
                rs.getString("sub_name"), rs.getLong("food_count")))
            .list();

        Map<String, List<Row>> byCategory = rows.stream()
            .collect(Collectors.groupingBy(Row::code, LinkedHashMap::new, Collectors.toList()));
        return byCategory.values().stream()
            .map(group -> new FoodCategory(
                group.getFirst().code(),
                group.getFirst().name(),
                group.stream().mapToLong(Row::count).sum(),
                group.stream()
                    .filter(row -> row.subCode() != null)
                    .map(row -> new FoodSubcategory(row.subCode(), row.subName(), row.count()))
                    .toList()))
            .toList();
    }

    public boolean exists(long id) {
        return jdbc.sql("SELECT count(*) FROM food WHERE id = :id").param("id", id).query(Long.class).single() > 0;
    }

    public long count() {
        return jdbc.sql("SELECT count(*) FROM food").query(Long.class).single();
    }
}
