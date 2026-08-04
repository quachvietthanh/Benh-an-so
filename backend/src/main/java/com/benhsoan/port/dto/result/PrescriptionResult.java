package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.prescription.enums.PrescriptionStatus;

public record PrescriptionResult(

        UUID id,

        String prescriptionCode,

        UUID medicalRecordId,

        PrescriptionStatus status,

        String note,

        UUID prescribedBy,

        Instant prescribedAt,

        UUID updatedBy,

        Instant updatedAt,

        List<PrescriptionItemResult> items,

        List<PrescriptionWarningResult> warnings

) {
}
