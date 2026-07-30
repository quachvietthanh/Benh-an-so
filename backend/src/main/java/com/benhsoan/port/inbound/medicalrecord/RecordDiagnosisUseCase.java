package com.benhsoan.port.inbound.medicalrecord;

import java.util.UUID;

import com.benhsoan.port.dto.command.medicalrecord.RecordDiagnosisCommand;
import com.benhsoan.port.dto.result.ExaminationDiagnosisResult;

public interface RecordDiagnosisUseCase {

    ExaminationDiagnosisResult recordDiagnosis(UUID examinationId, RecordDiagnosisCommand command);
}
