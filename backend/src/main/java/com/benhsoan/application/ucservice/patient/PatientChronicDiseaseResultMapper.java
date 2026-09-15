package com.benhsoan.application.ucservice.patient;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.patient.PatientChronicDisease;
import com.benhsoan.port.dto.result.patient.PatientChronicDiseaseResult;

@Component
public class PatientChronicDiseaseResultMapper {

    public PatientChronicDiseaseResult toResult(PatientChronicDisease chronicDisease) {
        return toResult(chronicDisease, null, null);
    }

    public PatientChronicDiseaseResult toResult(PatientChronicDisease chronicDisease, String diagnosisCode, String diagnosisName) {
        if (chronicDisease == null) {
            return null;
        }
        return PatientChronicDiseaseResult.builder()
                .id(chronicDisease.getId())
                .patientId(chronicDisease.getPatientId())
                .diagnosisCatalogId(chronicDisease.getDiagnosisCatalogId())
                .diagnosisCode(diagnosisCode)
                .diagnosisName(diagnosisName)
                .yearDetected(chronicDisease.getYearDetected())
                .notes(chronicDisease.getNotes())
                .active(chronicDisease.isActive())
                .createdBy(chronicDisease.getCreatedBy())
                .createdAt(chronicDisease.getCreatedAt())
                .updatedBy(chronicDisease.getUpdatedBy())
                .updatedAt(chronicDisease.getUpdatedAt())
                .build();
    }
}
