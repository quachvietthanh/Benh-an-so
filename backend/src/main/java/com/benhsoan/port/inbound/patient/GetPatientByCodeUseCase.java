package com.benhsoan.port.inbound.patient;

import com.benhsoan.port.dto.result.PatientResult;

public interface GetPatientByCodeUseCase {

    PatientResult getByCode(String patientCode);
}
