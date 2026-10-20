package com.mealplanner.food.ciqual;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.mealplanner.config.AppProperties;
import com.mealplanner.food.FoodRepository;

/**
 * Au démarrage : si la table des aliments est vide et qu'un fichier Ciqual est configuré
 * (app.ciqual.auto-import-path / variable CIQUAL_IMPORT_PATH), on l'importe.
 */
@Component
public class CiqualStartupImport implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CiqualStartupImport.class);

    private final AppProperties properties;
    private final FoodRepository foods;
    private final CiqualImportService importService;

    public CiqualStartupImport(AppProperties properties, FoodRepository foods, CiqualImportService importService) {
        this.properties = properties;
        this.foods = foods;
        this.importService = importService;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        String configuredPath = properties.ciqual().autoImportPath();
        if (configuredPath == null || configuredPath.isBlank()) {
            return;
        }
        if (foods.count() > 0) {
            log.info("Référentiel Ciqual déjà chargé, import automatique ignoré");
            return;
        }
        Path path = Path.of(configuredPath);
        if (!Files.isReadable(path)) {
            log.warn("Fichier Ciqual introuvable : {}", path.toAbsolutePath());
            return;
        }
        log.info("Import automatique du référentiel Ciqual depuis {}", path.toAbsolutePath());
        try (InputStream input = Files.newInputStream(path)) {
            importService.importWorkbook(input, path.getFileName().toString(), null);
        }
    }
}
