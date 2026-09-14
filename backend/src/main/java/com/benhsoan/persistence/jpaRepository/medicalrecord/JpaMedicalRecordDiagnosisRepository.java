package com.benhsoan.persistence.jpaRepository.medicalrecord;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordDiagnosisEntity;
import com.benhsoan.persistence.entity.visit.VisitEntity;

public interface JpaMedicalRecordDiagnosisRepository extends JpaRepository<MedicalRecordDiagnosisEntity, UUID> {

    List<MedicalRecordDiagnosisEntity> findByMedicalRecordId(UUID medicalRecordId);

    List<MedicalRecordDiagnosisEntity> findByMedicalRecordIdIn(Collection<UUID> medicalRecordIds);

    boolean existsByMedicalRecordId(UUID medicalRecordId);

    boolean existsByDiagnosisCatalogId(UUID diagnosisCatalogId);

    @Modifying
    void deleteByMedicalRecordId(UUID medicalRecordId);

    @Query("""
            SELECT d.diagnosisCatalogId
            FROM MedicalRecordDiagnosisEntity d
            JOIN MedicalRecordEntity mr ON mr.id = d.medicalRecordId
            JOIN VisitEntity v ON v.id = mr.visitId
            WHERE d.diagnosedBy = :doctorId
              AND d.diagnosisCatalogId IS NOT NULL
              AND v.status <> com.benhsoan.domain.visit.enums.VisitStatus.CANCELLED
            GROUP BY d.diagnosisCatalogId
            ORDER BY MAX(d.diagnosedAt) DESC
            """)
    List<UUID> findRecentCatalogIdsByDoctor(@Param("doctorId") UUID doctorId);

    @Query("""
            SELECT d.diagnosisCatalogId
            FROM MedicalRecordDiagnosisEntity d
            JOIN DiagnosisCatalogEntity c ON c.id = d.diagnosisCatalogId
            JOIN MedicalRecordEntity mr ON mr.id = d.medicalRecordId
            JOIN VisitEntity v ON v.id = mr.visitId
            WHERE v.specialtyId = :specialtyId
              AND v.status <> com.benhsoan.domain.visit.enums.VisitStatus.CANCELLED
              AND c.active = true
            GROUP BY d.diagnosisCatalogId, c.code
            ORDER BY COUNT(DISTINCT d.medicalRecordId) DESC, c.code ASC
            """)
    List<UUID> findPopularCatalogIdsBySpecialty(@Param("specialtyId") UUID specialtyId);
}
