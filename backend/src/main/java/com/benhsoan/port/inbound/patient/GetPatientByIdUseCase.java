package com.benhsoan.port.inbound.patient;

import java.util.UUID;

import com.benhsoan.port.dto.result.PatientResult;

public interface GetPatientByIdUseCase {

    PatientResult getById(UUID patientId);
}
