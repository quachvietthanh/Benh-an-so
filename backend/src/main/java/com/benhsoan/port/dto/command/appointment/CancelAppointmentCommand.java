package com.benhsoan.port.dto.command.appointment;

import lombok.Builder;

@Builder
public record CancelAppointmentCommand(

        String cancelReason

) {
}