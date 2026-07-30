package com.benhsoan.adapter.inbound.rest.request.medicalrecord;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateMedicalRecordRequest(
        @NotNull UUID visitId,
        String chiefComplaint, String symptoms, String medicalHistory,
        String physicalExamination, String clinicalProgress, String treatmentPlan,
        String doctorInstructions, String conclusion
) {
}
