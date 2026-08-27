package com.benhsoan.adapter.inbound.rest.request.appointment;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PatientRescheduleAppointmentRequest(

        @NotNull(message = "Ngày hẹn mới là bắt buộc.")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate newAppointmentDate,

        @NotNull(message = "Giờ hẹn mới là bắt buộc.")
        @JsonFormat(pattern = "HH:mm")
        LocalTime newStartTime,

        @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự.")
        String reason

) {
}
