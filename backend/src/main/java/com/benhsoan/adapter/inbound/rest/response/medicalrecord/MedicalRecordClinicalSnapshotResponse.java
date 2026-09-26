package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

import java.time.LocalDate;
import java.util.List;

public record MedicalRecordClinicalSnapshotResponse(
        String chiefComplaint,
        String symptoms,
        String medicalHistory,
        String physicalExamination,
        String clinicalProgress,
        String treatmentPlan,
        String doctorInstructions,
        String conclusion,
        LocalDate revisitDate,
        List<String> diagnoses
) {
    public MedicalRecordClinicalSnapshotResponse(
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
        this(chiefComplaint, symptoms, medicalHistory, physicalExamination,
                clinicalProgress, treatmentPlan, doctorInstructions, conclusion, null, diagnoses);
    }
}
