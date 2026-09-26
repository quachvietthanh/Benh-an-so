package com.benhsoan.adapter.inbound.rest.request.appointment;

import java.time.Instant;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AppointmentSeriesSessionRequest(
        @Min(value = 1, message = "Số thứ tự buổi khám phải từ 1 trở lên.")
        int sequenceNumber,

        @NotNull(message = "Thời gian bắt đầu buổi khám không được để trống.")
        Instant startTime,

        @NotNull(message = "Thời gian kết thúc buổi khám không được để trống.")
        Instant endTime
) {
}
