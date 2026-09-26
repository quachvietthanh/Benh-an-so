package com.benhsoan.port.inbound.medicalrecord;

import com.benhsoan.port.dto.command.medicalrecord.UpdateMedicalRecordTemplateStatusCommand;
import com.benhsoan.port.dto.result.MedicalRecordTemplateResult;

public interface UpdateMedicalRecordTemplateStatusUseCase {

    MedicalRecordTemplateResult updateStatus(UpdateMedicalRecordTemplateStatusCommand command);
}
