package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

public record PatientLoginResult(

        UUID userId,

        String username,

        String accessToken,

        String refreshToken,

        String role,

        Instant expiredAt,

        UUID patientId

) {
}
