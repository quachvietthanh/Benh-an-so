package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.List;

public record MedicalRecordCopyDocument(
        String clinicName,
        String clinicAddress,
        String clinicPhone,
        String patientCode,
        String patientName,
        String patientDateOfBirth,
        String patientGender,
        String visitCode,
        Instant visitAt,
        String doctorName,
        String chiefComplaint,
        String symptoms,
        String medicalHistory,
        String physicalExamination,
        String clinicalProgress,
        String treatmentPlan,
        String doctorInstructions,
        String conclusion,
        List<Diagnosis> diagnoses
) {
    public MedicalRecordCopyDocument {
        diagnoses = List.copyOf(diagnoses);
    }

    public record Diagnosis(String code, String name) {
    }
}
