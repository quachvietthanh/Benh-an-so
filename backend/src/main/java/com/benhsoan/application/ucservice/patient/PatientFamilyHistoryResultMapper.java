package com.benhsoan.application.ucservice.patient;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.patient.PatientFamilyHistory;
import com.benhsoan.port.dto.result.patient.PatientFamilyHistoryResult;

@Component
public class PatientFamilyHistoryResultMapper {

    public PatientFamilyHistoryResult toResult(PatientFamilyHistory familyHistory) {
        if (familyHistory == null) {
            return null;
        }
        return PatientFamilyHistoryResult.builder()
                .id(familyHistory.getId())
                .patientId(familyHistory.getPatientId())
                .relationship(familyHistory.getRelationship())
                .diagnosisCatalogId(familyHistory.getDiagnosisCatalogId())
                .notes(familyHistory.getNotes())
                .active(familyHistory.isActive())
                .createdBy(familyHistory.getCreatedBy())
                .createdAt(familyHistory.getCreatedAt())
                .updatedBy(familyHistory.getUpdatedBy())
                .updatedAt(familyHistory.getUpdatedAt())
                .build();
    }
}
