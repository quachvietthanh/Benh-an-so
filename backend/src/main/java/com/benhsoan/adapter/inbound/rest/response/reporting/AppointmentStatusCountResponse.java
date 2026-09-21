package com.benhsoan.adapter.inbound.rest.response.reporting;

import java.math.BigDecimal;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;

public record AppointmentStatusCountResponse(
        AppointmentStatus status,
        long count,
        BigDecimal percentage
) {
}
