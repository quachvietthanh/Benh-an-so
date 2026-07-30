package com.benhsoan.port.inbound.medicalrecord;

import java.util.UUID;

import com.benhsoan.port.dto.command.medicalrecord.UpdateMedicalRecordCommand;
import com.benhsoan.port.dto.result.MedicalRecordResult;

public interface UpdateMedicalRecordUseCase {
    MedicalRecordResult update(UUID medicalRecordId, UpdateMedicalRecordCommand command);
}
