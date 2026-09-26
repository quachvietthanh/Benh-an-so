package com.benhsoan.adapter.inbound.rest.request.medicalrecord;

import java.time.LocalDate;

public record UpdateMedicalRecordRequest(
        String chiefComplaint, String symptoms, String medicalHistory,
        String physicalExamination, String clinicalProgress, String treatmentPlan,
        String doctorInstructions, String conclusion, LocalDate revisitDate
) {
    public UpdateMedicalRecordRequest(
            String chiefComplaint, String symptoms, String medicalHistory,
            String physicalExamination, String clinicalProgress, String treatmentPlan,
            String doctorInstructions, String conclusion
    ) {
        this(chiefComplaint, symptoms, medicalHistory, physicalExamination,
                clinicalProgress, treatmentPlan, doctorInstructions, conclusion, null);
    }
}
