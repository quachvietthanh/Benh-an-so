package com.benhsoan.port.inbound.medicalrecord;

import com.benhsoan.port.dto.command.medicalrecord.UpdateMedicalRecordTemplateCommand;
import com.benhsoan.port.dto.result.MedicalRecordTemplateResult;

public interface UpdateMedicalRecordTemplateUseCase {

    MedicalRecordTemplateResult update(UpdateMedicalRecordTemplateCommand command);
}
