package com.benhsoan.adapter.inbound.rest.response.prescription;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.prescription.enums.PrescriptionStatus;

import lombok.Builder;

@Builder
public record PrescriptionResponse(

        UUID id,

        String prescriptionCode,

        UUID medicalRecordId,

        PrescriptionStatus status,

        String note,

        UUID prescribedBy,

        Instant prescribedAt,

        UUID updatedBy,

        Instant updatedAt,

        List<PrescriptionItemResponse> items,

        List<PrescriptionWarningResponse> warnings

) {
}
