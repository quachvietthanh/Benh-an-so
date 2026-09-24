package com.benhsoan.adapter.inbound.rest.request.patient;

import java.util.Set;

import com.benhsoan.domain.patient.enums.ConsentScope;

public record UpdatePatientConsentRequest(

        Boolean consentWithdrawn,

        String consentWithdrawnReason,

        Boolean consentAgreed,

        String consentVersion,

        Set<ConsentScope> scopes,

        Boolean requestDataErasure

) {
}
