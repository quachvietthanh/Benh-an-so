package com.benhsoan.application.ucservice.patient;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.patient.PatientFamilyHistory;
import com.benhsoan.port.dto.result.patient.PatientFamilyHistoryResult;

@Component
public class PatientFamilyHistoryResultMapper {

    public PatientFamilyHistoryResult toResult(PatientFamilyHistory familyHistory) {
        return toResult(familyHistory, null, null);
    }

    public PatientFamilyHistoryResult toResult(PatientFamilyHistory familyHistory, String diagnosisCode, String diagnosisName) {
        if (familyHistory == null) {
            return null;
        }
        return PatientFamilyHistoryResult.builder()
                .id(familyHistory.getId())
                .patientId(familyHistory.getPatientId())
                .relationship(familyHistory.getRelationship())
                .diagnosisCatalogId(familyHistory.getDiagnosisCatalogId())
                .diagnosisCode(diagnosisCode)
                .diagnosisName(diagnosisName)
                .notes(familyHistory.getNotes())
                .active(familyHistory.isActive())
                .createdBy(familyHistory.getCreatedBy())
                .createdAt(familyHistory.getCreatedAt())
                .updatedBy(familyHistory.getUpdatedBy())
                .updatedAt(familyHistory.getUpdatedAt())
                .build();
    }
}
