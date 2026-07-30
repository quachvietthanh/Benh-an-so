package com.benhsoan.port.outbound.repository.crudRepository.medicalrecord;

import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.port.outbound.repository.BaseRepository;

public interface DiagnosisCatalogRepository extends BaseRepository<DiagnosisCatalog, UUID> {

    List<DiagnosisCatalog> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code, String name);
}
