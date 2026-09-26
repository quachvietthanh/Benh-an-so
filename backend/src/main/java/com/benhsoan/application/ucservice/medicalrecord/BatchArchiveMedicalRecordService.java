package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.port.dto.result.BatchArchiveMedicalRecordResult;
import com.benhsoan.port.inbound.medicalrecord.BatchArchiveMedicalRecordUseCase;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.ArchiveMedicalRecordQueryRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BatchArchiveMedicalRecordService implements BatchArchiveMedicalRecordUseCase {

    private final MedicalRecordRepository medicalRecordRepository;
    private final VisitRepository visitRepository;
    private final ClinicConfigurationRepository clinicConfigurationRepository;
    private final ArchiveMedicalRecordQueryRepository archiveQueryRepository;
    private final MedicalRecordAuthorizationService authorizationService;
    private final MedicalRecordAccessAuditService accessAuditService;
    private final ClockPort clockPort;

    public static final int MAX_BATCH_SIZE = 100;

    @Override
    public BatchArchiveMedicalRecordResult batchArchive(List<UUID> medicalRecordIds, boolean archiveAllEligible) {
        if (!archiveAllEligible && medicalRecordIds != null && medicalRecordIds.size() > MAX_BATCH_SIZE) {
            throw new com.benhsoan.domain.shared.exception.ValidationException(
                    "Mỗi đợt lưu trữ hàng loạt chỉ xử lý tối đa " + MAX_BATCH_SIZE + " hồ sơ."
            );
        }

        UUID userId = authorizationService.requireArchiveManageAccess();

        int activeRecordDurationMonths = clinicConfigurationRepository.find()
                .map(ClinicConfiguration::getActiveRecordDurationMonths)
                .orElse(ClinicConfiguration.DEFAULT_ACTIVE_RECORD_DURATION_MONTHS);

        Instant now = clockPort.now();
        Instant completedBefore = now.atZone(ZoneOffset.UTC).minusMonths(activeRecordDurationMonths).toInstant();

        List<UUID> targetIds;
        if (archiveAllEligible) {
            targetIds = archiveQueryRepository.findAllEligibleMedicalRecordIds(completedBefore).stream()
                    .limit(MAX_BATCH_SIZE)
                    .toList();
        } else {
            targetIds = medicalRecordIds != null ? medicalRecordIds : List.of();
        }

        List<UUID> archivedIds = new ArrayList<>();
        List<UUID> skippedIds = new ArrayList<>();

        for (UUID recordId : targetIds) {
            Optional<MedicalRecord> recordOpt = medicalRecordRepository.findByIdForUpdate(recordId);
            if (recordOpt.isEmpty()) {
                skippedIds.add(recordId);
                continue;
            }

            MedicalRecord record = recordOpt.get();
            if (record.getStatus() != MedicalRecordStatus.SIGNED && record.getStatus() != MedicalRecordStatus.LOCKED) {
                skippedIds.add(recordId);
                continue;
            }

            Optional<Visit> visitOpt = visitRepository.findById(record.getVisitId());
            if (visitOpt.isEmpty()) {
                skippedIds.add(recordId);
                continue;
            }

            Visit visit = visitOpt.get();
            if (visit.getStatus() != VisitStatus.COMPLETED
                    || visit.getCompletedAt() == null
                    || visit.getCompletedAt().isAfter(completedBefore)) {
                skippedIds.add(recordId);
                continue;
            }

            record.archive(userId, now);
            MedicalRecord saved = medicalRecordRepository.save(record);
            accessAuditService.recordRecordAccess(
                    visit.getPatientId(), visit.getId(), saved.getId(), userId,
                    MedicalRecordAccessAction.ARCHIVE, "Batch medical record archived", now);
            archivedIds.add(saved.getId());
        }

        return BatchArchiveMedicalRecordResult.builder()
                .totalRequested(targetIds.size())
                .archivedCount(archivedIds.size())
                .skippedCount(skippedIds.size())
                .archivedMedicalRecordIds(archivedIds)
                .skippedMedicalRecordIds(skippedIds)
                .build();
    }
}
