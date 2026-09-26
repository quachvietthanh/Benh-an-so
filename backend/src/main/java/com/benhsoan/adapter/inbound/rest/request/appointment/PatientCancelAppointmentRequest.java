package com.benhsoan.adapter.inbound.rest.request.appointment;

import jakarta.validation.constraints.Size;

public record PatientCancelAppointmentRequest(

        @Size(max = 500, message = "Lý do hủy không được vượt quá 500 ký tự.")
        String cancellationReason

) {
}
