package com.benhsoan.port.dto.query.appointment;

import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.WaitlistStatus;

import lombok.Builder;

@Builder
public record GetAppointmentWaitlistQuery(
        UUID doctorId,
        LocalDate desiredDate,
        WaitlistStatus status
) {
}
