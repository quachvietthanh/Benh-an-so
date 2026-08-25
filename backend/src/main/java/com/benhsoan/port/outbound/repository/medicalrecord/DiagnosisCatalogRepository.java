package com.benhsoan.port.outbound.repository.medicalrecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
public interface DiagnosisCatalogRepository {

    DiagnosisCatalog save(DiagnosisCatalog diagnosisCatalog);

    Optional<DiagnosisCatalog> findById(UUID id);

    boolean existsByCode(String code);

    List<DiagnosisCatalog> search(String keyword, Boolean active);

    List<DiagnosisCatalog> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code, String name);
}
