package com.benhsoan.port.inbound.patient;

import com.benhsoan.port.dto.command.patient.MergePatientsCommand;
import com.benhsoan.port.dto.result.patient.MergePatientsResult;

public interface MergePatientsUseCase {

    MergePatientsResult merge(MergePatientsCommand command);
}
