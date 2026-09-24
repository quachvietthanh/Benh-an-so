package com.benhsoan.port.dto.result.patient;

import java.util.UUID;

import lombok.Builder;

@Builder
public record DataErasureResult(

        UUID patientId,

        boolean consentWithdrawn,

        boolean nonMedicalUseRestricted,

        boolean medicalRecordsRetained,

        int retentionYears,

        String message

) {
}
