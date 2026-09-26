package com.benhsoan.adapter.inbound.rest.response.reporting;

import java.util.UUID;

public record DoctorVisitItemResponse(
        Integer rank,
        UUID doctorId,
        String doctorCode,
        String doctorName,
        long totalVisits
) {
}
