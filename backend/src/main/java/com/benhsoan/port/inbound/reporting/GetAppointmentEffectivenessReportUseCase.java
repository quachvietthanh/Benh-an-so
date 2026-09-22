package com.benhsoan.port.inbound.reporting;

import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.port.dto.result.AppointmentEffectivenessReportResult;

public interface GetAppointmentEffectivenessReportUseCase {

    AppointmentEffectivenessReportResult getReport(
            LocalDate from,
            LocalDate to,
            UUID doctorId,
            String bookingChannel
    );
}
