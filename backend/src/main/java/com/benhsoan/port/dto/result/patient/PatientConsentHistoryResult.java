package com.benhsoan.port.dto.result.patient;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.benhsoan.domain.patient.enums.ConsentHistoryStatus;
import com.benhsoan.domain.patient.enums.ConsentScope;

import lombok.Builder;

@Builder
public record PatientConsentHistoryResult(

        UUID id,

        UUID patientId,

        int versionNumber,

        String versionCode,

        ConsentHistoryStatus status,

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
