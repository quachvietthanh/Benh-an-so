package com.benhsoan.adapter.inbound.rest.request.appointment;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAppointmentSeriesRequest(
        @NotNull(message = "ID bệnh nhân không được để trống.")
        UUID patientId,

        @NotNull(message = "ID bác sĩ không được để trống.")
        UUID doctorId,

        UUID medicalRecordId,

        @Size(max = 255, message = "Tiêu đề liệu trình tối đa 255 ký tự.")
        String title,

        @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự.")
        String notes,

        @Min(value = 2, message = "Số buổi của liệu trình phải từ 2 trở lên.")
        @Max(value = 50, message = "Số buổi của liệu trình tối đa là 50 buổi.")
        int totalSessions,

        @Min(value = 1, message = "Khoảng cách giữa các buổi phải từ 1 ngày trở lên.")
        @Max(value = 90, message = "Khoảng cách giữa các buổi tối đa là 90 ngày.")
        int intervalDays,

        @NotEmpty(message = "Danh sách các buổi của liệu trình không được để trống.")
        @Valid
        List<AppointmentSeriesSessionRequest> sessions
) {
}
