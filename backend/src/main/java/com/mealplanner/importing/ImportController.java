package com.mealplanner.importing;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mealplanner.common.NotFoundException;
import com.mealplanner.food.ciqual.CiqualImportService;
import com.mealplanner.user.CurrentUserService;

@RestController
public class ImportController {

    private final RecipeImportService recipeImports;
    private final CiqualImportService ciqualImports;
    private final ImportJobRepository jobs;
    private final CurrentUserService currentUser;

    public ImportController(RecipeImportService recipeImports, CiqualImportService ciqualImports,
                            ImportJobRepository jobs, CurrentUserService currentUser) {
        this.recipeImports = recipeImports;
        this.ciqualImports = ciqualImports;
        this.jobs = jobs;
        this.currentUser = currentUser;
    }

    /** Dépose un fichier de recettes ; le traitement continue en tâche de fond (suivre via GET /api/imports/{id}). */
    @PostMapping(path = "/api/imports/recipes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ImportJobResponse importRecipes(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide");
        }
        String filename = file.getOriginalFilename() == null ? "import" : file.getOriginalFilename();
        ImportJob job = recipeImports.createJob(currentUser.currentUserId(), filename);
        recipeImports.process(job.getId(), file.getBytes());
        return ImportJobResponse.from(job);
    }

    @GetMapping("/api/imports")
    public List<ImportJobResponse> myImports() {
        return jobs.findTop20ByUserIdOrderByCreatedAtDesc(currentUser.currentUserId()).stream()
            .map(ImportJobResponse::from)
            .toList();
    }

    @GetMapping("/api/imports/{id}")
    public ImportJobResponse get(@PathVariable Long id) {
        return jobs.findByIdAndUserId(id, currentUser.currentUserId())
            .map(ImportJobResponse::from)
            .orElseThrow(() -> new NotFoundException("Import introuvable : " + id));
    }

    /** Administration : (re)chargement du référentiel Ciqual. Synchrone (quelques secondes). */
    @PostMapping(path = "/api/admin/imports/ciqual", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportJobResponse importCiqual(@RequestParam("file") MultipartFile file) throws IOException {
        try (InputStream input = file.getInputStream()) {
            String filename = file.getOriginalFilename() == null ? "ciqual.xlsx" : file.getOriginalFilename();
            return ImportJobResponse.from(ciqualImports.importWorkbook(input, filename, currentUser.currentUserId()));
        }
    }

    @GetMapping("/api/admin/imports/ciqual")
    public List<ImportJobResponse> ciqualImports() {
        return jobs.findTop20ByTypeOrderByCreatedAtDesc(ImportJob.Type.CIQUAL).stream()
            .map(ImportJobResponse::from)
            .toList();
    }
}
