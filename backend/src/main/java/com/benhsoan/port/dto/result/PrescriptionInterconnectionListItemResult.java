package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.prescription.enums.InterconnectionStatus;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;

public record PrescriptionInterconnectionListItemResult(
        UUID prescriptionId,
        String prescriptionCode,
        UUID patientId,
        String patientCode,
        String patientName,
        UUID prescribedBy,
        String doctorName,
        PrescriptionStatus prescriptionStatus,
        InterconnectionStatus interconnectionStatus,
        Instant lastInterconnectionAt,
        String lastInterconnectionError,
        String receiptCode
) {
}
