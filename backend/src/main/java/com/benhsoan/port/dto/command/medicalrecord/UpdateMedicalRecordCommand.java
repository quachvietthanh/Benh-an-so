package com.benhsoan.port.dto.command.medicalrecord;

public record UpdateMedicalRecordCommand(
        String chiefComplaint, String symptoms, String medicalHistory,
        String physicalExamination, String clinicalProgress, String treatmentPlan,
        String doctorInstructions, String conclusion
) {
}
