package com.benhsoan.application.ucservice.reporting;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.reporting.enums.ReportType;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.DiseasePatternReportResult;
import com.benhsoan.port.inbound.reporting.GetDiseasePatternReportUseCase;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetDiseasePatternReportService implements GetDiseasePatternReportUseCase {

    private static final String MANAGER_ROLE = "MANAGER";

    private final OperationalReportDataService operationalReportDataService;
    private final UserRepository userRepository;
    private final CurrentUserPort currentUserPort;
    private final OperationalReportAuditService operationalReportAuditService;

    @Override
    public DiseasePatternReportResult getDiseasePatternReport(LocalDate from, LocalDate to, UUID doctorId) {
        ensureAuthorized();

        String doctorName = null;
        if (doctorId != null) {
            User doctor = userRepository.findById(doctorId)
                    .orElseThrow(() -> new ValidationException("Doctor not found."));
            doctorName = doctor.getFullName();
        }

        return operationalReportDataService.getDiseasePatterns(from, to, doctorId, doctorName);
    }

    private void ensureAuthorized() {
        if (!currentUserPort.hasRole(MANAGER_ROLE)) {
            operationalReportAuditService.logAccessDenied(
                    ReportType.DISEASE_PATTERN_REPORT,
                    "Only managers can view the disease pattern report."
            );
            throw new AccessDeniedException(
                    "Only managers can view the disease pattern report."
            );
        }
    }
}
