package com.benhsoan.port.dto.command.medicalrecord;

import java.time.LocalDate;

public record UpdateInstructionsAndTreatmentPlanCommand(
        String treatmentPlan,
        String doctorInstructions,
        LocalDate revisitDate
) {
}
