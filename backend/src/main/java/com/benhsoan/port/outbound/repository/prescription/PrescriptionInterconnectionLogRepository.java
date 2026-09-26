package com.benhsoan.port.outbound.repository.prescription;

import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.prescription.PrescriptionInterconnectionLog;

public interface PrescriptionInterconnectionLogRepository {

    PrescriptionInterconnectionLog save(PrescriptionInterconnectionLog log);

    List<PrescriptionInterconnectionLog> findByPrescriptionId(UUID prescriptionId);
}
