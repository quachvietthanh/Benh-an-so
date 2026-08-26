package com.benhsoan.port.dto.result;

import java.util.UUID;

public record PatientPortalRegistrationResult(

        UUID userId,

        UUID patientId,

        String patientCode,

        String phone,

        String fullName,

        String accessToken,

        String refreshToken,

        String tokenType

) {
}
