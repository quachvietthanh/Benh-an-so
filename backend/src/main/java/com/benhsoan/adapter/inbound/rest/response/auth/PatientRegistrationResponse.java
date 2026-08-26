package com.benhsoan.adapter.inbound.rest.response.auth;

import java.util.UUID;

public record PatientRegistrationResponse(

        UUID userId,

        UUID patientId,

        String phone,

        String fullName,

        String accessToken,

        String refreshToken,

        String tokenType

) {
}
