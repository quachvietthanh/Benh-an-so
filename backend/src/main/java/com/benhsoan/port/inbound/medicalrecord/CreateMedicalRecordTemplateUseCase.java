package com.benhsoan.port.inbound.medicalrecord;

import com.benhsoan.port.dto.command.medicalrecord.CreateMedicalRecordTemplateCommand;
import com.benhsoan.port.dto.result.MedicalRecordTemplateResult;

public interface CreateMedicalRecordTemplateUseCase {

    MedicalRecordTemplateResult create(CreateMedicalRecordTemplateCommand command);
}
