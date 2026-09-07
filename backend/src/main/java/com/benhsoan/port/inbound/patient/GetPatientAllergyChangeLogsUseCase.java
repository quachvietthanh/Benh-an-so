package com.benhsoan.port.inbound.patient;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.patient.PatientAllergyChangeLogResult;

public interface GetPatientAllergyChangeLogsUseCase {
    List<PatientAllergyChangeLogResult> getChangeLogs(UUID patientId, UUID allergyId);
}
