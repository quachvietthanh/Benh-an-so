package com.benhsoan.port.dto.command.medicalrecord;

import java.util.UUID;

public record CreateMedicalRecordCommand(
        UUID visitId, String chiefComplaint, String symptoms, String medicalHistory,
        String physicalExamination, String clinicalProgress, String treatmentPlan,
        String doctorInstructions, String conclusion
) {
}
