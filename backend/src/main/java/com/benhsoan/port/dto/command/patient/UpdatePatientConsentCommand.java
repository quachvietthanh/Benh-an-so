package com.benhsoan.port.dto.command.patient;

import java.util.Set;

import com.benhsoan.domain.patient.enums.ConsentScope;

import lombok.Builder;

@Builder
public record UpdatePatientConsentCommand(

        Boolean consentAgreed,

        Boolean consentWithdrawn,

        String consentWithdrawnReason,

        String consentVersion,

        Set<ConsentScope> scopes,

        Boolean requestDataErasure

) {
}
