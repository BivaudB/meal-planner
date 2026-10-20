package com.mealplanner.importing;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Suivi d'un import de fichier (recettes d'un utilisateur ou référentiel Ciqual). */
@Entity
@Table(name = "import_job")
public class ImportJob {

    public enum Type { RECIPES, CIQUAL }

    public enum Status { PENDING, RUNNING, COMPLETED, COMPLETED_WITH_ERRORS, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null pour un import système (Ciqual au démarrage). */
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    private int totalItems;
    private int importedItems;
    private int errorItems;

    @Column(columnDefinition = "text")
    private String report;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant startedAt;
    private Instant finishedAt;

    protected ImportJob() {
    }

    public ImportJob(Long userId, Type type, String filename) {
        this.userId = userId;
        this.type = type;
        this.filename = filename;
    }

    public void start() {
        status = Status.RUNNING;
        startedAt = Instant.now();
    }

    public void finish(int total, int imported, int errors, String report) {
        this.totalItems = total;
        this.importedItems = imported;
        this.errorItems = errors;
        this.report = report;
        this.status = errors == 0 ? Status.COMPLETED : Status.COMPLETED_WITH_ERRORS;
        this.finishedAt = Instant.now();
    }

    public void fail(String report) {
        this.report = report;
        this.status = Status.FAILED;
        this.finishedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Type getType() { return type; }
    public String getFilename() { return filename; }
    public Status getStatus() { return status; }
    public int getTotalItems() { return totalItems; }
    public int getImportedItems() { return importedItems; }
    public int getErrorItems() { return errorItems; }
    public String getReport() { return report; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
}
