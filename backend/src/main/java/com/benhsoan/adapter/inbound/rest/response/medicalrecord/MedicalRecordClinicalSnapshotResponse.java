package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

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
        List<String> diagnoses
) {
}
