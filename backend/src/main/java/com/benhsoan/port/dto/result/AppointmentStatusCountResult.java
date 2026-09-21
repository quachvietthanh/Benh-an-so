package com.benhsoan.port.dto.result;

import java.math.BigDecimal;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;

public record AppointmentStatusCountResult(
        String bookingChannel,
        AppointmentStatus status,
        long count,
        BigDecimal percentage
) {
}
