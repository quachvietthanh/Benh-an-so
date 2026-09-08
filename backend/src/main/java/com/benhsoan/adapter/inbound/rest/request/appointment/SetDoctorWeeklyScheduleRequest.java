package com.benhsoan.adapter.inbound.rest.request.appointment;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record SetDoctorWeeklyScheduleRequest(
        @NotNull(message = "doctorId is required.")
        UUID doctorId,

        @NotNull(message = "items is required.")
        @Valid
        List<DoctorWeeklyScheduleItemRequest> items
) {
}
