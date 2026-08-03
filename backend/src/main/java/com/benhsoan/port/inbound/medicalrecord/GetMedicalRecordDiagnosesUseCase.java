package com.benhsoan.port.inbound.medicalrecord;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.MedicalRecordDiagnosisResult;

public interface GetMedicalRecordDiagnosesUseCase {

    List<MedicalRecordDiagnosisResult> getByMedicalRecordId(UUID medicalRecordId);
}
