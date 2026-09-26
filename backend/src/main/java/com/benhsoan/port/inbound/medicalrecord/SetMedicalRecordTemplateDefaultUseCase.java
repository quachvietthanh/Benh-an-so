package com.benhsoan.port.inbound.medicalrecord;

import java.util.UUID;

import com.benhsoan.port.dto.result.MedicalRecordTemplateResult;

public interface SetMedicalRecordTemplateDefaultUseCase {

    MedicalRecordTemplateResult setDefault(UUID templateId);
}
