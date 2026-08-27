package com.benhsoan.adapter.inbound.rest.request.patient;

public record UpdatePatientConsentRequest(

        Boolean consentWithdrawn,

        String consentWithdrawnReason,

        Boolean consentAgreed,

        String consentVersion

) {
}
