package com.benhsoan.adapter.inbound.rest.response.patient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PatientMedicalHistoryDetailResponse(
        UUID visitId,
        Instant visitAt,
        String doctorName,
        String specialtyName,
        List<DiagnosisResponse> diagnoses,
        List<PrescriptionItemResponse> prescriptionItems,
        String doctorAdvice
) {

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
