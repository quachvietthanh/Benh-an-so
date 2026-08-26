package com.benhsoan.persistence.jpaRepository.medicalrecord;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.medicalrecord.DiagnosisCatalogEntity;

public interface JpaDiagnosisCatalogRepository extends JpaRepository<DiagnosisCatalogEntity, UUID> {

    boolean existsByCode(String code);

    @Query("""
            SELECT diagnosis
            FROM DiagnosisCatalogEntity diagnosis
            WHERE (:keyword IS NULL
                    OR LOWER(diagnosis.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(diagnosis.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:active IS NULL OR diagnosis.active = :active)
            ORDER BY diagnosis.diseaseGroup ASC, diagnosis.code ASC
            """)
    List<DiagnosisCatalogEntity> search(
            @Param("keyword") String keyword,
            @Param("active") Boolean active
    );

    List<DiagnosisCatalogEntity> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code, String name);
}
