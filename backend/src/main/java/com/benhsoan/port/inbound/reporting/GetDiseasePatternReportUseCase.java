package com.benhsoan.port.inbound.reporting;

import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.port.dto.result.DiseasePatternReportResult;

public interface GetDiseasePatternReportUseCase {

    DiseasePatternReportResult getDiseasePatternReport(LocalDate from, LocalDate to, UUID doctorId);
}
