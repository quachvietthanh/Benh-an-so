package com.benhsoan.application.ucservice.reporting;

import java.time.LocalDate;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.result.DoctorVisitsReportResult;
import com.benhsoan.port.inbound.reporting.GetDoctorVisitsReportUseCase;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetDoctorVisitsReportService implements GetDoctorVisitsReportUseCase {

    private static final String MANAGER_ROLE = "MANAGER";

    private final OperationalReportDataService operationalReportDataService;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public DoctorVisitsReportResult getDoctorVisits(LocalDate from, LocalDate to) {
        ensureAuthorized();

        DoctorVisitsReportResult result = operationalReportDataService.getDoctorVisits(from, to);
        return new DoctorVisitsReportResult(
                result.from(),
                result.to(),
                clockPort.now(),
                result.items()
        );
    }

    private void ensureAuthorized() {
        if (!currentUserPort.hasRole(MANAGER_ROLE)) {
            throw new AccessDeniedException(
                    "Only managers can view the doctor visits report."
            );
        }
    }
}
