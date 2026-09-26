package com.benhsoan.application.ucservice.patient;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.patient.PatientAllergy;
import com.benhsoan.domain.patient.PatientAllergyChangeLog;
import com.benhsoan.port.dto.result.patient.PatientAllergyChangeLogResult;
import com.benhsoan.port.dto.result.patient.PatientAllergyResult;

@Component
public class PatientAllResultMapper {

    public PatientAllergyResult toResult(PatientAllergy allergy) {
        if (allergy == null) {
            return null;
        }

        return PatientAllergyResult.builder()
                .id(allergy.getId())
                .patientId(allergy.getPatientId())
                .allergenType(allergy.getAllergenType())
                .allergenName(allergy.getAllergenName())
                .severity(allergy.getSeverity())
                .reaction(allergy.getReaction())
                .notes(allergy.getNotes())
                .active(allergy.isActive())
                .createdBy(allergy.getCreatedBy())
                .createdAt(allergy.getCreatedAt())
                .updatedBy(allergy.getUpdatedBy())
                .updatedAt(allergy.getUpdatedAt())
                .build();
    }

    public PatientAllergyChangeLogResult toResult(PatientAllergyChangeLog changeLog) {
        if (changeLog == null) {
            return null;
        }

        return PatientAllergyChangeLogResult.builder()
                .id(changeLog.getId())
                .allergyId(changeLog.getAllergyId())
                .patientId(changeLog.getPatientId())
                .action(changeLog.getAction())
                .beforeData(changeLog.getBeforeData())
                .afterData(changeLog.getAfterData())
                .changeReason(changeLog.getChangeReason())
                .changedBy(changeLog.getChangedBy())
                .changedAt(changeLog.getChangedAt())
                .build();
    }
}
