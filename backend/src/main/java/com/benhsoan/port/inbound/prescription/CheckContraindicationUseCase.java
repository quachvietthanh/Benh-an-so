package com.benhsoan.port.inbound.prescription;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.ContraindicationCheckResult;

public interface CheckContraindicationUseCase {

    ContraindicationCheckResult check(UUID medicalRecordId, List<UUID> medicineIds);

    ContraindicationCheckResult checkByPatientId(UUID patientId, List<UUID> medicineIds);
}
