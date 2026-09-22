package com.benhsoan.adapter.inbound.rest.response.controlledmedicine;

import java.time.Instant;
import java.util.UUID;

public record ControlledMedicineRegisterResponse(
        UUID id,
        UUID prescriptionId,
        UUID prescriptionItemId,
        UUID medicineId,
        String medicineName,
        UUID patientId,
        String patientCode,
        String patientName,
        UUID prescribedBy,
        String doctorName,
        UUID dispensedBy,
        String pharmacistName,
        int quantity,
        Instant dispensedAt
) {
}
