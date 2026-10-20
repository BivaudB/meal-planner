package com.mealplanner.importing;

import java.time.Instant;

public record ImportJobResponse(
    Long id,
    ImportJob.Type type,
    String filename,
    ImportJob.Status status,
    int totalItems,
    int importedItems,
    int errorItems,
    String report,
    Instant createdAt,
    Instant finishedAt) {

    public static ImportJobResponse from(ImportJob job) {
        return new ImportJobResponse(job.getId(), job.getType(), job.getFilename(), job.getStatus(),
            job.getTotalItems(), job.getImportedItems(), job.getErrorItems(), job.getReport(),
            job.getCreatedAt(), job.getFinishedAt());
    }
}
