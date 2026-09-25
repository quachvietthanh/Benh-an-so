package com.benhsoan.adapter.inbound.rest.response.patient;

import java.util.UUID;

import lombok.Builder;

@Builder
public record DataErasureResponse(

        UUID patientId,

        boolean consentWithdrawn,

        boolean nonMedicalUseRestricted,

        boolean medicalRecordsRetained,

        int retentionYears,

        String message

) {
}
