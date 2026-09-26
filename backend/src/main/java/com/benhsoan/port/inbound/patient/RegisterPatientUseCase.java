package com.benhsoan.port.inbound.patient;

import com.benhsoan.port.dto.command.patient.RegisterPatientCommand;
import com.benhsoan.port.dto.result.PatientResult;

public interface RegisterPatientUseCase {

    PatientResult register(RegisterPatientCommand command);

}