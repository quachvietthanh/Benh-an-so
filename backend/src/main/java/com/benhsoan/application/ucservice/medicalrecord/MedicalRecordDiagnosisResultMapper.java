package com.benhsoan.application.ucservice.medicalrecord;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.medicalrecord.MedicalRecordDiagnosis;
import com.benhsoan.port.dto.result.MedicalRecordDiagnosisResult;

@Component
public class MedicalRecordDiagnosisResultMapper {

    public MedicalRecordDiagnosisResult toResult(MedicalRecordDiagnosis diagnosis) {
        return new MedicalRecordDiagnosisResult(diagnosis.getId(), diagnosis.getMedicalRecordId(),
                diagnosis.getDiagnosisCode(), diagnosis.getDiagnosisName(), diagnosis.getDiagnosisType(),
                diagnosis.getNote(), diagnosis.getDiagnosedBy(), diagnosis.getDiagnosedAt());
    }
}
