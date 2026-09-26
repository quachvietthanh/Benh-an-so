package com.benhsoan.port.dto.command.medicalrecord;

import java.time.LocalDate;
import java.util.UUID;

public record CreateMedicalRecordCommand(
        UUID visitId, String chiefComplaint, String symptoms, String medicalHistory,
        String physicalExamination, String clinicalProgress, String treatmentPlan,
        String doctorInstructions, String conclusion, LocalDate revisitDate
) {
    public CreateMedicalRecordCommand(
            UUID visitId, String chiefComplaint, String symptoms, String medicalHistory,
            String physicalExamination, String clinicalProgress, String treatmentPlan,
            String doctorInstructions, String conclusion
    ) {
        this(visitId, chiefComplaint, symptoms, medicalHistory, physicalExamination,
                clinicalProgress, treatmentPlan, doctorInstructions, conclusion, null);
    }
}
