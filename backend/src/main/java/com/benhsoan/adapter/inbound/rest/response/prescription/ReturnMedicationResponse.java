package com.benhsoan.adapter.inbound.rest.response.prescription;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.prescription.enums.PrescriptionStatus;

public record ReturnMedicationResponse(
        UUID prescriptionId,
        PrescriptionStatus status,
        UUID returnedBy,
        Instant returnedAt,
        List<ReturnedMedicationItemResponse> returns
) {
}
