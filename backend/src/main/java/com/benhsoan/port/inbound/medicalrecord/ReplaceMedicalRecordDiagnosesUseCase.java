package com.benhsoan.port.inbound.medicalrecord;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.command.medicalrecord.ReplaceMedicalRecordDiagnosesCommand;
import com.benhsoan.port.dto.result.MedicalRecordDiagnosisResult;

public interface ReplaceMedicalRecordDiagnosesUseCase {

    List<MedicalRecordDiagnosisResult> replace(
            UUID medicalRecordId,
            ReplaceMedicalRecordDiagnosesCommand command
    );
}
