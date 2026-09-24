package com.benhsoan.domain.appointment.exception;

import java.util.List;

import com.benhsoan.domain.appointment.AppointmentSeriesConflictDetail;
import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class AppointmentSeriesConflictException extends AppointmentException {

    private final List<AppointmentSeriesConflictDetail> conflicts;

    public AppointmentSeriesConflictException(List<AppointmentSeriesConflictDetail> conflicts) {
        super(
                DomainErrorCode.APPOINTMENT_SERIES_CONFLICT,
                "Một hoặc nhiều buổi trong liệu trình bị vướng ngày nghỉ hoặc trùng lịch."
        );
        this.conflicts = conflicts != null ? List.copyOf(conflicts) : List.of();
    }

    public List<AppointmentSeriesConflictDetail> getConflicts() {
        return conflicts;
    }
}
