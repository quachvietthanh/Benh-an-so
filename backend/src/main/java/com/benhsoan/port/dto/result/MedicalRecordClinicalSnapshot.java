package com.benhsoan.port.dto.result;

import java.util.List;

/**
 * Structured clinical content of a medical record version (NCL-11-CN-003).
 * Populated for the original record (Version 1) so it can be compared against amendments.
 */
public record MedicalRecordClinicalSnapshot(
        String chiefComplaint,
        String symptoms,
        String medicalHistory,
        String physicalExamination,
        String clinicalProgress,
        String treatmentPlan,
        String doctorInstructions,
        String conclusion,
        List<String> diagnoses
) {
    public MedicalRecordClinicalSnapshot {
        diagnoses = List.copyOf(diagnoses);
    }
}
