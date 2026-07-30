package com.benhsoan.adapter.inbound.rest.request.medicalrecord;

public record UpdateMedicalRecordRequest(
        String chiefComplaint, String symptoms, String medicalHistory,
        String physicalExamination, String clinicalProgress, String treatmentPlan,
        String doctorInstructions, String conclusion
) {
}
