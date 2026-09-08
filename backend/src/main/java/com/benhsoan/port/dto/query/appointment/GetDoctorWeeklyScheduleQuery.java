package com.benhsoan.port.dto.query.appointment;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.ValidationException;

public record GetDoctorWeeklyScheduleQuery(UUID doctorId) {

    public GetDoctorWeeklyScheduleQuery {
        if (doctorId == null) {
            throw new ValidationException("doctorId is required.");
        }
    }
}
