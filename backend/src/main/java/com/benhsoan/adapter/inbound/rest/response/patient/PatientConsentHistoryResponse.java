package com.benhsoan.adapter.inbound.rest.response.patient;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.benhsoan.domain.patient.enums.ConsentScope;

import lombok.Builder;

@Builder
public record PatientConsentHistoryResponse(

        UUID id,

        UUID patientId,

        int versionNumber,

        String versionCode,

        String status,

        String statusDescription,

        Set<ConsentScope> scopes,

        boolean consentAgreed,

        Instant consentAgreedAt,

        boolean consentWithdrawn,

        Instant consentWithdrawnAt,

        String consentWithdrawnReason,

        boolean nonMedicalUseRestricted,

        String signerName,

        UUID createdBy,

        Instant createdAt

) {
}
