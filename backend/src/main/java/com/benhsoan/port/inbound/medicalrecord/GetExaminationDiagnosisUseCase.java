package com.benhsoan.port.inbound.medicalrecord;

import java.util.UUID;

import com.benhsoan.port.dto.result.ExaminationDiagnosisResult;

public interface GetExaminationDiagnosisUseCase {

    ExaminationDiagnosisResult getDiagnosis(UUID examinationId);
}
