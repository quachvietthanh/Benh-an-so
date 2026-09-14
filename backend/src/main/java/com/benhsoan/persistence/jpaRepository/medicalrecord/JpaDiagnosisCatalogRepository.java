package com.benhsoan.persistence.jpaRepository.medicalrecord;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
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
                    OR LOWER(diagnosis.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR (diagnosis.abbreviation IS NOT NULL
                        AND LOWER(diagnosis.abbreviation) LIKE LOWER(CONCAT('%', :keyword, '%'))))
              AND (:active IS NULL OR diagnosis.active = :active)
            ORDER BY diagnosis.diseaseGroup ASC, diagnosis.code ASC
            """)
    List<DiagnosisCatalogEntity> search(
            @Param("keyword") String keyword,
            @Param("active") Boolean active
    );

    @Query("""
            SELECT d
            FROM DiagnosisCatalogEntity d
            WHERE d.active = true
              AND (:diseaseGroup IS NULL OR d.diseaseGroup = :diseaseGroup)
              AND (
                  LOWER(d.code) = :kw
                  OR d.nameNorm = :kw
                  OR (d.abbreviationNorm IS NOT NULL AND d.abbreviationNorm = :kw)
                  OR LOWER(d.code) LIKE CONCAT(:kw, '%')
                  OR d.nameNorm LIKE CONCAT(:kw, '%')
                  OR (d.abbreviationNorm IS NOT NULL AND d.abbreviationNorm LIKE CONCAT(:kw, '%'))
                  OR LOWER(d.code) LIKE CONCAT('%', :kw, '%')
                  OR d.nameNorm LIKE CONCAT('%', :kw, '%')
                  OR (d.abbreviationNorm IS NOT NULL AND d.abbreviationNorm LIKE CONCAT('%', :kw, '%'))
              )
            ORDER BY
              CASE
                WHEN LOWER(d.code) = :kw THEN 0
                WHEN d.nameNorm = :kw THEN 1
                WHEN d.abbreviationNorm = :kw THEN 2
                WHEN LOWER(d.code) LIKE CONCAT(:kw, '%') THEN 3
                WHEN d.nameNorm LIKE CONCAT(:kw, '%') THEN 4
                WHEN d.abbreviationNorm LIKE CONCAT(:kw, '%') THEN 5
                WHEN LOWER(d.code) LIKE CONCAT('%', :kw, '%') THEN 6
                WHEN d.nameNorm LIKE CONCAT('%', :kw, '%') THEN 7
                WHEN d.abbreviationNorm LIKE CONCAT('%', :kw, '%') THEN 8
                ELSE 9
              END ASC,
              d.code ASC
            """)
    List<DiagnosisCatalogEntity> searchActiveByKeyword(
            @Param("kw") String normalizedKeyword,
            @Param("diseaseGroup") String diseaseGroup,
            Pageable pageable
    );

    List<DiagnosisCatalogEntity> findByActiveTrueAndDiseaseGroupOrderByCodeAsc(
            String diseaseGroup,
            Pageable pageable
    );

    List<DiagnosisCatalogEntity> findByNameNormIsNull();

    List<DiagnosisCatalogEntity> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code, String name);

    List<DiagnosisCatalogEntity> findByActiveOrderByCodeAsc(boolean active);
}
