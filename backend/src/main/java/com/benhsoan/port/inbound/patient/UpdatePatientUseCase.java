package com.benhsoan.port.inbound.patient;

import java.util.UUID;

import com.benhsoan.port.dto.command.patient.UpdatePatientCommand;
import com.benhsoan.port.dto.result.PatientResult;
public interface UpdatePatientUseCase {

    PatientResult update(
            UUID patientId,
            UpdatePatientCommand command
    );

}