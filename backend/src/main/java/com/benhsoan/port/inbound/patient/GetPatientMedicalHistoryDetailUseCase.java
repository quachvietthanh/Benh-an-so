package com.benhsoan.port.inbound.patient;

import java.util.UUID;

import com.benhsoan.port.dto.result.patient.PatientMedicalHistoryDetailResult;

public interface GetPatientMedicalHistoryDetailUseCase {

    PatientMedicalHistoryDetailResult getMedicalHistoryDetail(UUID visitId);

}
