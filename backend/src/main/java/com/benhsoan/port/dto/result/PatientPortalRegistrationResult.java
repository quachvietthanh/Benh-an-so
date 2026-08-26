package com.benhsoan.port.dto.result;

import java.util.UUID;

public record PatientPortalRegistrationResult(

        UUID userId,

        UUID patientId,

        String phone,

        String fullName

) {
}
