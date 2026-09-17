package com.benhsoan.adapter.inbound.rest.request.medicalrecord;

import java.time.LocalDate;

public record UpdateInstructionsAndTreatmentPlanRequest(
        String treatmentPlan,
        String doctorInstructions,
        LocalDate revisitDate
) {
}
