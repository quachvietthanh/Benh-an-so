package com.benhsoan.port.outbound.repository.medicalrecord;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
public interface DiagnosisCatalogRepository {

    DiagnosisCatalog save(DiagnosisCatalog diagnosisCatalog);

    Optional<DiagnosisCatalog> findById(UUID id);

    Optional<DiagnosisCatalog> findByCode(String code);

    List<DiagnosisCatalog> findAllByIds(Collection<UUID> ids);

    boolean existsByCode(String code);

    List<DiagnosisCatalog> search(String keyword, Boolean active);

    List<DiagnosisCatalog> searchActive(String normalizedKeyword, String diseaseGroup, int limit);

    List<DiagnosisCatalog> findByActiveAndDiseaseGroup(String diseaseGroup, int limit);

    List<DiagnosisCatalog> findAllByActive(boolean active);

    List<DiagnosisCatalog> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code, String name);
}
