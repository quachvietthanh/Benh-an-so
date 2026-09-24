package com.benhsoan.port.inbound.patient;

import java.util.UUID;

import com.benhsoan.port.dto.command.patient.UpdatePatientConsentCommand;
import com.benhsoan.port.dto.result.PatientResult;

public interface UpdatePatientConsentUseCase {

    PatientResult updateConsent(UUID patientId, UpdatePatientConsentCommand command);

}
