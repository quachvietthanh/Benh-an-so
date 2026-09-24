package com.benhsoan.port.inbound.patient;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.patient.PatientConsentHistoryResult;

public interface GetPatientConsentHistoryUseCase {

    List<PatientConsentHistoryResult> getConsentHistory(UUID patientId);

}
