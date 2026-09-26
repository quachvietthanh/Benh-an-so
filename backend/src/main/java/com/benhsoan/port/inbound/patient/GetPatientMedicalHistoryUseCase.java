package com.benhsoan.port.inbound.patient;

import java.util.List;

import com.benhsoan.port.dto.result.patient.PatientMedicalHistorySummaryResult;

public interface GetPatientMedicalHistoryUseCase {

    List<PatientMedicalHistorySummaryResult> getMedicalHistory();

}
