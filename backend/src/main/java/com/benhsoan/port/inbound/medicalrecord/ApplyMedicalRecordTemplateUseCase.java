package com.benhsoan.port.inbound.medicalrecord;

import java.util.UUID;

import com.benhsoan.port.dto.command.medicalrecord.ApplyMedicalRecordTemplateCommand;
import com.benhsoan.port.dto.result.MedicalRecordResult;

public interface ApplyMedicalRecordTemplateUseCase {

    MedicalRecordResult apply(UUID medicalRecordId, ApplyMedicalRecordTemplateCommand command);
}
