package com.benhsoan.persistence.adapterRepository.medicalrecord;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.benhsoan.domain.shared.VietnameseTextNormalizer;
import com.benhsoan.persistence.entity.medicalrecord.DiagnosisCatalogEntity;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaDiagnosisCatalogRepository;

import lombok.RequiredArgsConstructor;

/**
 * Backfills the accent-insensitive {@code name_norm}/{@code abbreviation_norm}
 * columns for catalog rows that predate the V48 migration. Vietnamese
 * diacritic stripping must happen in Java, so this runs once at startup and is
 * idempotent (only rows with a NULL {@code name_norm} are touched).
 */
@Component
@RequiredArgsConstructor
public class DiagnosisCatalogNormalizationBackfill implements CommandLineRunner {

    private final JpaDiagnosisCatalogRepository repository;
    private final PlatformTransactionManager transactionManager;

    @Override
    public void run(String... args) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> backfill());
    }

    private void backfill() {
        List<DiagnosisCatalogEntity> rows = repository.findByNameNormIsNull();
        if (rows.isEmpty()) {
            return;
        }
        for (DiagnosisCatalogEntity row : rows) {
            row.setNameNorm(VietnameseTextNormalizer.normalize(row.getName()));
            row.setAbbreviationNorm(VietnameseTextNormalizer.normalize(row.getAbbreviation()));
        }
        repository.saveAll(rows);
    }
}
