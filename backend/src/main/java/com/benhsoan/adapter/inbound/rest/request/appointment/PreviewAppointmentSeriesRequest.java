package com.benhsoan.adapter.inbound.rest.request.appointment;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PreviewAppointmentSeriesRequest(
        @NotNull(message = "ID bệnh nhân không được để trống.")
        UUID patientId,

        @NotNull(message = "ID bác sĩ không được để trống.")
        UUID doctorId,

        @NotNull(message = "Thời gian bắt đầu buổi đầu tiên không được để trống.")
        Instant firstSessionStartTime,

        @Min(value = 1, message = "Thời lượng buổi khám tối thiểu là 1 phút.")
        @Max(value = 480, message = "Thời lượng buổi khám tối đa là 480 phút.")
        int sessionDurationMinutes,

        @Min(value = 2, message = "Số buổi của liệu trình phải từ 2 trở lên.")
        @Max(value = 50, message = "Số buổi của liệu trình tối đa là 50 buổi.")
        int totalSessions,

        @Min(value = 1, message = "Khoảng cách giữa các buổi phải từ 1 ngày trở lên.")
        @Max(value = 90, message = "Khoảng cách giữa các buổi tối đa là 90 ngày.")
        int intervalDays
) {
}
