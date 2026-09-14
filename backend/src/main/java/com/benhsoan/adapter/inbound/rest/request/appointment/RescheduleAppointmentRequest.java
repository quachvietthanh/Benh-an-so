package com.benhsoan.adapter.inbound.rest.request.appointment;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RescheduleAppointmentRequest(

        UUID newDoctorId,

        @NotNull(message = "Thời gian bắt đầu là bắt buộc.")
        @Future(message = "Thời gian bắt đầu phải ở trong tương lai.")
        Instant startTime,

        @NotNull(message = "Thời gian kết thúc là bắt buộc.")
        @Future(message = "Thời gian kết thúc phải ở trong tương lai.")
        Instant endTime,

        @NotBlank(message = "Lý do dời lịch là bắt buộc.")
        @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự.")
        String reason

) {
}
