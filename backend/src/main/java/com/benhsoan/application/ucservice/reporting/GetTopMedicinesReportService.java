package com.benhsoan.application.ucservice.reporting;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.result.TopMedicinesReportResult;
import com.benhsoan.port.inbound.reporting.GetTopMedicinesReportUseCase;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetTopMedicinesReportService implements GetTopMedicinesReportUseCase {

    private final OperationalReportDataService operationalReportDataService;
    private final ClockPort clockPort;

    @Override
    public TopMedicinesReportResult getTopMedicines(LocalDate from, LocalDate to) {
        TopMedicinesReportResult result = operationalReportDataService.getTopMedicines(from, to);
        return new TopMedicinesReportResult(
                result.from(),
                result.to(),
                clockPort.now(),
                result.items()
        );
    }
}
