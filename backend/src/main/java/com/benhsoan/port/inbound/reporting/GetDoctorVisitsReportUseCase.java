package com.benhsoan.port.inbound.reporting;

import java.time.LocalDate;

import com.benhsoan.port.dto.result.DoctorVisitsReportResult;

public interface GetDoctorVisitsReportUseCase {

    DoctorVisitsReportResult getDoctorVisits(LocalDate from, LocalDate to);
}
