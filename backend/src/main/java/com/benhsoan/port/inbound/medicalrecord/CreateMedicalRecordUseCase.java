package com.benhsoan.port.inbound.medicalrecord;

import com.benhsoan.port.dto.command.medicalrecord.CreateMedicalRecordCommand;
import com.benhsoan.port.dto.result.MedicalRecordResult;

public interface CreateMedicalRecordUseCase {
    MedicalRecordResult create(CreateMedicalRecordCommand command);
}
