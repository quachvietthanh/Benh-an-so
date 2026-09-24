package com.benhsoan.port.dto.query.appointment;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import lombok.Builder;

@Builder
public record GetWaitlistSuggestionQuery(
        UUID doctorId,
        LocalDate desiredDate
) {
    public GetWaitlistSuggestionQuery {
        Objects.requireNonNull(doctorId, "ID bác sĩ không được để trống");
        Objects.requireNonNull(desiredDate, "Ngày khám không được để trống");
    }
}
