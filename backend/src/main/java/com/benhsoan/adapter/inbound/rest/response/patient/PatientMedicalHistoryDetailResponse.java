package com.benhsoan.adapter.inbound.rest.response.patient;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PatientMedicalHistoryDetailResponse(
        UUID visitId,
        Instant visitAt,
        String doctorName,
        String specialtyName,
        List<DiagnosisResponse> diagnoses,
        List<PrescriptionItemResponse> prescriptionItems,
        String doctorAdvice,
        String treatmentPlan,
        LocalDate revisitDate
) {
    public PatientMedicalHistoryDetailResponse(
            UUID visitId,
            Instant visitAt,
            String doctorName,
            String specialtyName,
            List<DiagnosisResponse> diagnoses,
            List<PrescriptionItemResponse> prescriptionItems,
            String doctorAdvice
    ) {
        this(visitId, visitAt, doctorName, specialtyName, diagnoses, prescriptionItems, doctorAdvice, null, null);
    }

    public record DiagnosisResponse(String icd10Code, String name) {
    }

    public record PrescriptionItemResponse(
            String medicineName,
            int quantity,
            String dosage,
            String instructions
    ) {
    }
}
