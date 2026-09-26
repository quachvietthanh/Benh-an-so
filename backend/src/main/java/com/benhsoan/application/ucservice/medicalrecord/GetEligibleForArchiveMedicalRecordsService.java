package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Instant;
import java.time.ZoneOffset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.port.dto.result.ArchiveEligibleMedicalRecordResult;
import com.benhsoan.port.inbound.medicalrecord.GetEligibleForArchiveMedicalRecordsUseCase;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.ArchiveMedicalRecordQueryRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetEligibleForArchiveMedicalRecordsService implements GetEligibleForArchiveMedicalRecordsUseCase {

    private final ArchiveMedicalRecordQueryRepository archiveQueryRepository;
    private final ClinicConfigurationRepository clinicConfigurationRepository;
    private final MedicalRecordAuthorizationService authorizationService;
    private final ClockPort clockPort;

    @Override
    public Page<ArchiveEligibleMedicalRecordResult> getEligibleRecords(Pageable pageable) {
        authorizationService.requireArchiveManageAccess();

        int activeRecordDurationMonths = clinicConfigurationRepository.find()
                .map(ClinicConfiguration::getActiveRecordDurationMonths)
                .orElse(ClinicConfiguration.DEFAULT_ACTIVE_RECORD_DURATION_MONTHS);

        Instant now = clockPort.now();
        Instant completedBefore = now.atZone(ZoneOffset.UTC).minusMonths(activeRecordDurationMonths).toInstant();

        return archiveQueryRepository.findEligibleForArchive(completedBefore, pageable);
    }
}
