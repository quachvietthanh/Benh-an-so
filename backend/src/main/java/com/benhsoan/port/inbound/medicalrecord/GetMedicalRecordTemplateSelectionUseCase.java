package com.benhsoan.port.inbound.medicalrecord;

import java.util.UUID;

import com.benhsoan.port.dto.result.MedicalRecordTemplateSelectionResult;

public interface GetMedicalRecordTemplateSelectionUseCase {

    MedicalRecordTemplateSelectionResult getForMedicalRecord(UUID medicalRecordId);

    MedicalRecordTemplateSelectionResult getForVisit(UUID visitId);
}
