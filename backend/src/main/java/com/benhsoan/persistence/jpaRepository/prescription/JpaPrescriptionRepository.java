package com.benhsoan.persistence.jpaRepository.prescription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.prescription.PrescriptionEntity;

import jakarta.persistence.LockModeType;

public interface JpaPrescriptionRepository
        extends JpaRepository<PrescriptionEntity, UUID> {

    Optional<PrescriptionEntity> findByPrescriptionCode(String prescriptionCode);

    boolean existsByPrescriptionCode(String prescriptionCode);

    Optional<PrescriptionEntity> findTopByOrderByPrescriptionCodeDesc();

    List<PrescriptionEntity> findByMedicalRecordIdOrderByPrescribedAtDesc(
            UUID medicalRecordId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select prescription from PrescriptionEntity prescription "
            + "where prescription.id = :id")
    Optional<PrescriptionEntity> findByIdForUpdate(@Param("id") UUID id);
}
