package com.benhsoan.port.inbound.patient;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.patient.PatientFamilyHistoryResult;

public interface GetPatientFamilyHistoryUseCase {
    List<PatientFamilyHistoryResult> getFamilyHistory(UUID patientId);
}
