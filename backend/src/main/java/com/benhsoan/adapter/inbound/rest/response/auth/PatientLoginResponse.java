package com.benhsoan.adapter.inbound.rest.response.auth;

import java.time.Instant;
import java.util.UUID;

public record PatientLoginResponse(

        UUID userId,

        String username,

        String accessToken,

        String refreshToken,

        String role,

        Instant expiredAt,

        UUID patientId

) {
}
