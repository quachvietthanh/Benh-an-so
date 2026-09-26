package com.benhsoan.application.ucservice.reporting;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.result.OperationalTimelineResult;
import com.benhsoan.port.inbound.reporting.GetOperationalTimelineUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetOperationalTimelineService implements GetOperationalTimelineUseCase {

    private final OperationalReportDataService operationalReportDataService;

    @Override
    public OperationalTimelineResult getTimeline(LocalDate from, LocalDate to) {
        return operationalReportDataService.getTimeline(from, to);
    }
}
