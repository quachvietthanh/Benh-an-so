package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.port.dto.command.medicalrecord.GetOverdueMedicalRecordsQuery;
import com.benhsoan.port.dto.result.OverdueMedicalRecordResult;
import com.benhsoan.port.inbound.medicalrecord.GetOverdueMedicalRecordsUseCase;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.OverdueMedicalRecordQueryRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetOverdueMedicalRecordsService implements GetOverdueMedicalRecordsUseCase {

    private final ClinicConfigurationRepository clinicConfigurationRepository;
    private final OverdueMedicalRecordQueryRepository overdueQueryRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public Page<OverdueMedicalRecordResult> getOverdueRecords(GetOverdueMedicalRecordsQuery query) {
        UUID targetDoctorId = query != null ? query.doctorId() : null;
        if (currentUserPort.hasRole("DOCTOR") && !currentUserPort.hasRole("ADMIN") && !currentUserPort.hasRole("MANAGER")) {
            targetDoctorId = currentUserPort.getCurrentUserId();
        }

        int signingDeadlineHours = clinicConfigurationRepository.find()
                .map(ClinicConfiguration::getSigningDeadlineHours)
                .orElse(ClinicConfiguration.DEFAULT_SIGNING_DEADLINE_HOURS);

        Instant now = clockPort.now();
        Instant thresholdCompletedAt = now.minus(Duration.ofHours(signingDeadlineHours));
        Pageable pageable = (query != null && query.pageable() != null)
                ? query.pageable()
                : PageRequest.of(0, 20);

        return overdueQueryRepository.findOverdueRecords(
                targetDoctorId,
                thresholdCompletedAt,
                signingDeadlineHours,
                now,
                pageable
        );
    }
}
