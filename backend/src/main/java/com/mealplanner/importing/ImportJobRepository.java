package com.mealplanner.importing;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {

    List<ImportJob> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

    List<ImportJob> findTop20ByTypeOrderByCreatedAtDesc(ImportJob.Type type);

    Optional<ImportJob> findByIdAndUserId(Long id, Long userId);
}
