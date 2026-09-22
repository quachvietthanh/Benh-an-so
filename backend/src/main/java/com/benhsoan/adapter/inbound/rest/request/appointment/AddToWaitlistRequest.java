package com.benhsoan.adapter.inbound.rest.request.appointment;

import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.TimePreference;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddToWaitlistRequest(
        @NotNull(message = "ID bệnh nhân không được để trống.")
        UUID patientId,

        @NotNull(message = "ID bác sĩ không được để trống.")
        UUID doctorId,

        @NotNull(message = "Ngày mong muốn khám không được để trống.")
        LocalDate desiredDate,

        TimePreference timePreference,

        @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự.")
        String note
) {
}
