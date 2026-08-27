package com.benhsoan.adapter.inbound.rest.request.appointment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PatientBookAppointmentRequest(

        @NotNull
        UUID doctorId,

        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate appointmentDate,

        @NotNull
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,

        @Size(max = 500, message = "Lý do khám không được vượt quá 500 ký tự.")
        String reason

) {
}
