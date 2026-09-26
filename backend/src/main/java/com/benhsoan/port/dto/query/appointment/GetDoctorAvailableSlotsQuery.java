package com.benhsoan.port.dto.query.appointment;

import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.shared.exception.ValidationException;

public record GetDoctorAvailableSlotsQuery(UUID doctorId, LocalDate date) {

    public GetDoctorAvailableSlotsQuery {
        if (doctorId == null) {
            throw new ValidationException("doctorId is required.");
        }
        if (date == null) {
            throw new ValidationException("date is required.");
        }
    }

}
