package com.benhsoan.adapter.inbound.rest.request.appointment;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

public record WeeklyScheduleItemRequest(
        @NotNull(message = "Ngày trong tuần không được để trống.")
        DayOfWeek dayOfWeek,

        @NotNull(message = "Giờ bắt đầu không được để trống.")
        LocalTime startTime,

        @NotNull(message = "Giờ kết thúc không được để trống.")
        LocalTime endTime,

        Boolean active
) {
    public boolean isActive() {
        return active == null || active;
    }
}
