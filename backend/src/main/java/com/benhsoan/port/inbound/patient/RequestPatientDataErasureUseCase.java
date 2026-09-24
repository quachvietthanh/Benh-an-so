package com.benhsoan.port.inbound.patient;

import java.util.UUID;

import com.benhsoan.port.dto.command.patient.RequestPatientDataErasureCommand;
import com.benhsoan.port.dto.result.patient.DataErasureResult;

public interface RequestPatientDataErasureUseCase {

    DataErasureResult requestErasure(UUID patientId, RequestPatientDataErasureCommand command);

}
