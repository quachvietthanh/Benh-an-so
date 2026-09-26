package com.benhsoan.port.inbound.prescription;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.PatientAllergyWarningResult;

public interface CheckPatientDrugAllergyUseCase {

    List<PatientAllergyWarningResult> check(UUID medicalRecordId, List<UUID> medicineIds);

    List<PatientAllergyWarningResult> checkByPatientId(UUID patientId, List<UUID> medicineIds);
}
