package com.benhsoan.port.outbound.repository.prescription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.prescription.PrescriptionWarningLog;

public interface PrescriptionWarningLogRepository {

    PrescriptionWarningLog save(PrescriptionWarningLog warningLog);

    Optional<PrescriptionWarningLog> findById(UUID id);

    List<PrescriptionWarningLog> findByPrescriptionId(UUID prescriptionId);
}
