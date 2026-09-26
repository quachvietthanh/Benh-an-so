package com.benhsoan.adapter.inbound.rest.response.patient;

import java.time.LocalDate;
import java.util.UUID;

/**
 * NCL-14-CN-010 CV-02: response of {@code GET /patient-portal/patients/linked}. Minimal
 * non-clinical field set only - no allergies, chronic disease, medical history, identity
 * number, insurance number, phone or address are exposed.
 */
public record LinkedPatientProfileResponse(
        UUID patientId,
        String patientCode,
        String fullName,
        LocalDate dateOfBirth,
        int age,
        boolean isMinor,
        String relationship,
        boolean self,
        boolean requiresGuardianLinkReview
) {
}
