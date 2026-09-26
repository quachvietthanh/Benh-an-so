package com.benhsoan.port.dto.result.patient;

import java.time.LocalDate;
import java.util.UUID;

/**
 * NCL-14-CN-010 CV-02: minimal, non-clinical description of a patient profile the
 * authenticated portal account may act for (own profile, or a linked dependent).
 * Deliberately excludes all clinical data and direct identifiers (phone, address, identity
 * number, insurance number).
 */
public record LinkedPatientProfileResult(
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
