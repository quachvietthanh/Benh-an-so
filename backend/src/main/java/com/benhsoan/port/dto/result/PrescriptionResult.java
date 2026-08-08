package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.prescription.enums.PrescriptionStatus;

public record PrescriptionResult(

        UUID id,

        String prescriptionCode,

        UUID medicalRecordId,

        UUID visitId,

        String visitCode,

        UUID patientId,

        String patientCode,

        String patientName,

        PrescriptionStatus status,

        String note,

        UUID prescribedBy,

        String doctorName,

        Instant prescribedAt,

        UUID updatedBy,

        Instant updatedAt,

        List<PrescriptionItemResult> items,

        List<PrescriptionWarningResult> warnings

) {
}
