package com.benhsoan.adapter.inbound.rest.request.appointment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotNull;

public record PatientBookAppointmentRequest(

        @NotNull
        UUID doctorId,

        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate appointmentDate,

        @NotNull
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,

        String reason

) {
}
