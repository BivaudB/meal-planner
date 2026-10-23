package com.mealplanner.food.ciqual;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.mealplanner.config.AppProperties;
import com.mealplanner.food.FoodService;

/**
 * Au démarrage : si la table des aliments est vide et qu'un fichier Ciqual est disponible
 * (app.ciqual.auto-import-path / variable CIQUAL_IMPORT_PATH, par défaut ../data/ciqual.xlsx
 * téléchargé par docker compose), on l'importe. S'exécute avant le catalogue de recettes.
 */
@Component
@Order(1)
public class CiqualStartupImport implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CiqualStartupImport.class);

    private final AppProperties properties;
    private final FoodService foods;
    private final CiqualImportService importService;

    public CiqualStartupImport(AppProperties properties, FoodService foods, CiqualImportService importService) {
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
        Optional<Path> found = locate(Path.of(configuredPath));
        if (found.isEmpty()) {
            log.warn("Fichier Ciqual introuvable : {} (lancez « docker compose up -d » pour le télécharger)",
                Path.of(configuredPath).toAbsolutePath().normalize());
            return;
        }
        Path path = found.get();
        log.info("Import automatique du référentiel Ciqual depuis {}", path.toAbsolutePath().normalize());
        try (InputStream input = Files.newInputStream(path)) {
            importService.importWorkbook(input, path.getFileName().toString(), null);
        }
    }

    /**
     * Le chemin par défaut est relatif au dossier backend/ ("../data/ciqual.xlsx") ; si le backend
     * est lancé depuis la racine du dépôt (IDE), on essaie aussi "data/ciqual.xlsx".
     */
    static Optional<Path> locate(Path configured) {
        List<Path> candidates = configured.isAbsolute()
            ? List.of(configured)
            : List.of(configured, Path.of("data").resolve(configured.getFileName()));
        return candidates.stream().filter(Files::isReadable).findFirst();
    }
}
