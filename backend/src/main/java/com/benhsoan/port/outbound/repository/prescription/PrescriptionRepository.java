package com.benhsoan.port.outbound.repository.prescription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
public interface PrescriptionRepository {

    Prescription save(Prescription prescription);

    Optional<Prescription> findById(UUID id);

    Optional<Prescription> findByPrescriptionCode(String prescriptionCode);

    boolean existsByPrescriptionCode(String prescriptionCode);

    Optional<Prescription> findTopByOrderByPrescriptionCodeDesc();

    List<Prescription> findByMedicalRecordId(UUID medicalRecordId);

    Page<Prescription> findByStatus(PrescriptionStatus status, Pageable pageable);

    Optional<Prescription> findByIdForUpdate(UUID id);
}
