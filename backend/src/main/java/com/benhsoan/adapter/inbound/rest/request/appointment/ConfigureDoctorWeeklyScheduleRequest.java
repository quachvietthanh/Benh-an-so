package com.benhsoan.adapter.inbound.rest.request.appointment;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/**
 * Request to configure a doctor's weekly working schedule.
 * Applies collection replacement semantics: any days previously active in the doctor's weekly schedule
 * that are omitted from this payload will automatically be deactivated (active = false).
 */
public record ConfigureDoctorWeeklyScheduleRequest(
        @NotEmpty(message = "Danh sách lịch làm việc không được để trống.")
        @Valid
        List<WeeklyScheduleItemRequest> schedules
) {
}
