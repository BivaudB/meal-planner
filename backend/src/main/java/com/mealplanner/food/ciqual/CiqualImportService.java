package com.mealplanner.food.ciqual;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.mealplanner.food.ciqual.CiqualValueParser.ParsedValue;
import com.mealplanner.food.ciqual.CiqualWorkbookReader.CiqualData;
import com.mealplanner.food.ciqual.CiqualWorkbookReader.FoodGroup;
import com.mealplanner.food.ciqual.CiqualWorkbookReader.FoodRow;
import com.mealplanner.food.ciqual.CiqualWorkbookReader.NutrientDefinition;
import com.mealplanner.importing.ImportJob;
import com.mealplanner.importing.ImportJobRepository;

/**
 * Charge (ou recharge) le référentiel Ciqual en base. L'import est idempotent :
 * les aliments existants sont mis à jour, leurs valeurs nutritionnelles remplacées.
 * Environ 3 500 aliments x 75 nutriments : quelques secondes en insertions par lots.
 */
@Service
public class CiqualImportService {

    private static final Logger log = LoggerFactory.getLogger(CiqualImportService.class);
    private static final int BATCH_SIZE = 1000;

    // Codes Ciqual (ORIGCPCD) des valeurs clés dénormalisées dans la table food
    private static final int ENERGY_KJ = 327;
    private static final int ENERGY_KCAL = 328;
    private static final int PROTEIN = 25000;
    private static final int CARBOHYDRATE = 31000;
    private static final int SUGARS = 32000;
    private static final int FAT = 40000;
    private static final int SATURATED_FAT = 40302;
    private static final int FIBER = 34100;
    private static final int SALT = 10004;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final ImportJobRepository jobs;

    public CiqualImportService(JdbcTemplate jdbc, TransactionTemplate transaction, ImportJobRepository jobs) {
        this.jdbc = jdbc;
        this.transaction = transaction;
        this.jobs = jobs;
    }

    /** Import synchrone, suivi dans import_job. */
    public ImportJob importWorkbook(InputStream input, String filename, Long userId) {
        ImportJob job = new ImportJob(userId, ImportJob.Type.CIQUAL, filename);
        job.start();
        job = jobs.save(job);
        try {
            CiqualData data = new CiqualWorkbookReader().read(input);
            transaction.executeWithoutResult(status -> write(data));
            String report = data.warnings().isEmpty() ? null : String.join("\n", data.warnings());
            job.finish(data.foods().size(), data.foods().size(), 0, report);
            log.info("Import Ciqual terminé : {} aliments, {} nutriments, {} groupes",
                data.foods().size(), data.nutrients().size(), data.groups().size());
        } catch (IOException | RuntimeException e) {
            log.error("Échec de l'import Ciqual", e);
            job.fail(e.getMessage());
        }
        return jobs.save(job);
    }

    private void write(CiqualData data) {
        jdbc.batchUpdate("""
                INSERT INTO nutrient (id, infoods_tag, name, unit, sort_order) VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET infoods_tag = EXCLUDED.infoods_tag, name = EXCLUDED.name,
                    unit = EXCLUDED.unit, sort_order = EXCLUDED.sort_order
                """,
            data.nutrients(), BATCH_SIZE, (ps, n) -> {
                ps.setInt(1, n.id());
                ps.setString(2, n.infoodsTag());
                ps.setString(3, n.name());
                ps.setString(4, n.unit());
                ps.setInt(5, n.sortOrder());
            });

        // Les groupes parents sont listés avant leurs enfants (ordre de lecture)
        jdbc.batchUpdate("""
                INSERT INTO food_group (code, level, parent_code, name) VALUES (?, ?, ?, ?)
                ON CONFLICT (code) DO UPDATE SET level = EXCLUDED.level, parent_code = EXCLUDED.parent_code,
                    name = EXCLUDED.name
                """,
            sortedByLevel(data.groups()), BATCH_SIZE, (ps, g) -> {
                ps.setString(1, g.code());
                ps.setInt(2, g.level());
                ps.setString(3, g.parentCode());
                ps.setString(4, g.name());
            });

        jdbc.batchUpdate("""
                INSERT INTO food (id, name, scientific_name, group_code, energy_kcal, energy_kj, protein_g,
                    carbohydrate_g, sugars_g, fat_g, saturated_fat_g, fiber_g, salt_g, source, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'CIQUAL', now())
                ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, scientific_name = EXCLUDED.scientific_name,
                    group_code = EXCLUDED.group_code, energy_kcal = EXCLUDED.energy_kcal,
                    energy_kj = EXCLUDED.energy_kj, protein_g = EXCLUDED.protein_g,
                    carbohydrate_g = EXCLUDED.carbohydrate_g, sugars_g = EXCLUDED.sugars_g, fat_g = EXCLUDED.fat_g,
                    saturated_fat_g = EXCLUDED.saturated_fat_g, fiber_g = EXCLUDED.fiber_g,
                    salt_g = EXCLUDED.salt_g, updated_at = now()
                """,
            data.foods(), BATCH_SIZE, (ps, f) -> {
                ps.setLong(1, f.id());
                ps.setString(2, f.name());
                ps.setString(3, f.scientificName());
                ps.setString(4, f.groupCode());
                ps.setBigDecimal(5, key(f, ENERGY_KCAL));
                ps.setBigDecimal(6, key(f, ENERGY_KJ));
                ps.setBigDecimal(7, key(f, PROTEIN));
                ps.setBigDecimal(8, key(f, CARBOHYDRATE));
                ps.setBigDecimal(9, key(f, SUGARS));
                ps.setBigDecimal(10, key(f, FAT));
                ps.setBigDecimal(11, key(f, SATURATED_FAT));
                ps.setBigDecimal(12, key(f, FIBER));
                ps.setBigDecimal(13, key(f, SALT));
            });

        record Value(long foodId, int nutrientId, ParsedValue value) {
        }
        List<Value> values = new ArrayList<>();
        for (FoodRow food : data.foods()) {
            for (Map.Entry<Integer, ParsedValue> entry : food.nutrients().entrySet()) {
                values.add(new Value(food.id(), entry.getKey(), entry.getValue()));
            }
        }
        jdbc.update("DELETE FROM food_nutrient WHERE food_id IN (SELECT id FROM food WHERE source = 'CIQUAL')");
        jdbc.batchUpdate("""
                INSERT INTO food_nutrient (food_id, nutrient_id, value, qualifier, raw_value) VALUES (?, ?, ?, ?, ?)
                """,
            values, BATCH_SIZE, (ps, v) -> {
                ps.setLong(1, v.foodId());
                ps.setInt(2, v.nutrientId());
                ps.setBigDecimal(3, v.value().value());
                ps.setString(4, v.value().qualifier());
                ps.setString(5, truncate(v.value().raw(), 50));
            });
    }

    private static List<FoodGroup> sortedByLevel(List<FoodGroup> groups) {
        return groups.stream().sorted((a, b) -> Integer.compare(a.level(), b.level())).toList();
    }

    private static BigDecimal key(FoodRow food, int nutrientId) {
        ParsedValue value = food.nutrients().get(nutrientId);
        return value == null ? null : value.forCalculation();
    }

    private static String truncate(String text, int max) {
        return text == null || text.length() <= max ? text : text.substring(0, max);
    }
}
