package com.benhsoan.port.dto.result.patient;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PatientMedicalHistoryDetailResult(
        UUID visitId,
        Instant visitAt,
        String doctorName,
        String specialtyName,
        List<DiagnosisItem> diagnoses,
        List<PrescriptionItemView> prescriptionItems,
        String doctorAdvice,
        String treatmentPlan,
        LocalDate revisitDate
) {
    public PatientMedicalHistoryDetailResult(
            UUID visitId,
            Instant visitAt,
            String doctorName,
            String specialtyName,
            List<DiagnosisItem> diagnoses,
            List<PrescriptionItemView> prescriptionItems,
            String doctorAdvice
    ) {
        this(visitId, visitAt, doctorName, specialtyName, diagnoses, prescriptionItems, doctorAdvice, null, null);
    }

    public record DiagnosisItem(String icd10Code, String name) {
    }

    public record PrescriptionItemView(
            String medicineName,
            int quantity,
            String dosage,
            String instructions
    ) {
    }
}
