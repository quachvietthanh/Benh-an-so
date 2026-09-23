package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

public record ControlledMedicineRegisterResult(
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
