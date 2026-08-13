package com.benhsoan.application.ucservice.reporting;

import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.result.OperationalSummaryResult;
import com.benhsoan.port.inbound.reporting.GetOperationalSummaryUseCase;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetOperationalSummaryService implements GetOperationalSummaryUseCase {

    private final OperationalReportDataService operationalReportDataService;

    @Override
    public OperationalSummaryResult getSummary(LocalDate from, LocalDate to) {
        return operationalReportDataService.getSummary(from, to);
    }
}
