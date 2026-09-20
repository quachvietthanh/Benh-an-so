package com.benhsoan.port.inbound.patient;

import java.util.UUID;

import com.benhsoan.domain.patient.enums.PregnancyStatus;
import com.benhsoan.port.dto.result.PatientResult;

public interface UpdatePatientPregnancyStatusUseCase {

    PatientResult update(UUID patientId, PregnancyStatus pregnancyStatus);
}
