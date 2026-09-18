package com.benhsoan.adapter.inbound.rest.response.visit;

import java.util.UUID;

public record HandoverDoctorResponse(
        UUID id,
        String fullName
) {
}
