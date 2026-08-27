package com.benhsoan.port.dto.result.patient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PatientMedicalHistoryDetailResult(
        UUID visitId,
        Instant visitAt,
        String doctorName,
        String specialtyName,
        List<DiagnosisItem> diagnoses,
        List<PrescriptionItemView> prescriptionItems,
        String doctorAdvice
) {

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
