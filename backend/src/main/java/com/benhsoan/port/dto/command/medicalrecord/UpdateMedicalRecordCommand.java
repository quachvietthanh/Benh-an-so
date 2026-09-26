package com.benhsoan.port.dto.command.medicalrecord;

import java.time.LocalDate;

public record UpdateMedicalRecordCommand(
        String chiefComplaint, String symptoms, String medicalHistory,
        String physicalExamination, String clinicalProgress, String treatmentPlan,
        String doctorInstructions, String conclusion, LocalDate revisitDate
) {
    public UpdateMedicalRecordCommand(
            String chiefComplaint, String symptoms, String medicalHistory,
            String physicalExamination, String clinicalProgress, String treatmentPlan,
            String doctorInstructions, String conclusion
    ) {
        this(chiefComplaint, symptoms, medicalHistory, physicalExamination,
                clinicalProgress, treatmentPlan, doctorInstructions, conclusion, null);
    }
}
