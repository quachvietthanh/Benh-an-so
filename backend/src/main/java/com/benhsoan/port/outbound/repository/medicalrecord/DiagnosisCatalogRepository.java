package com.benhsoan.port.outbound.repository.medicalrecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
public interface DiagnosisCatalogRepository {

    Optional<DiagnosisCatalog> findById(UUID id);

    List<DiagnosisCatalog> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code, String name);

    List<DiagnosisCatalog> findAll();
}
