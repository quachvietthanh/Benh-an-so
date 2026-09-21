package com.benhsoan.port.outbound.repository.prescription;

import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.prescription.PrescriptionContraindicationWarningLog;

public interface PrescriptionContraindicationWarningLogRepository {

    PrescriptionContraindicationWarningLog save(PrescriptionContraindicationWarningLog log);

    List<PrescriptionContraindicationWarningLog> findByPrescriptionId(UUID prescriptionId);
}
