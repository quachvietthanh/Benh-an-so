package com.benhsoan.port.outbound.repository.prescription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.prescription.Prescription;
public interface PrescriptionRepository {

    Optional<Prescription> findByPrescriptionCode(String prescriptionCode);

    boolean existsByPrescriptionCode(String prescriptionCode);

    Optional<Prescription> findTopByOrderByPrescriptionCodeDesc();

    List<Prescription> findByMedicalRecordId(UUID medicalRecordId);

    Optional<Prescription> findByIdForUpdate(UUID id);
}
