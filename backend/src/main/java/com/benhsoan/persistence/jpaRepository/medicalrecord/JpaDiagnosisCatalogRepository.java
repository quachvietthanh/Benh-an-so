package com.benhsoan.persistence.jpaRepository.medicalrecord;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.medicalrecord.DiagnosisCatalogEntity;

public interface JpaDiagnosisCatalogRepository extends JpaRepository<DiagnosisCatalogEntity, UUID> {

    List<DiagnosisCatalogEntity> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code, String name);
}
