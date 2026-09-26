package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotEligibleForArchiveException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.result.MedicalRecordResult;
import com.benhsoan.port.inbound.medicalrecord.ArchiveMedicalRecordUseCase;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ArchiveMedicalRecordService implements ArchiveMedicalRecordUseCase {

    private final MedicalRecordRepository medicalRecordRepository;
    private final VisitRepository visitRepository;
    private final ClinicConfigurationRepository clinicConfigurationRepository;
    private final MedicalRecordAuthorizationService authorizationService;
    private final MedicalRecordAccessAuditService accessAuditService;
    private final MedicalRecordTemplateApplicationMapper templateMapper;
    private final MedicalRecordResultMapper resultMapper;
    private final ClockPort clockPort;

    @Override
    public MedicalRecordResult archive(UUID medicalRecordId) {
        UUID userId = authorizationService.requireArchiveManageAccess();
        MedicalRecord record = medicalRecordRepository.findByIdForUpdate(medicalRecordId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(medicalRecordId));
        Visit visit = visitRepository.findById(record.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(record.getVisitId()));

        int activeRecordDurationMonths = clinicConfigurationRepository.find()
                .map(ClinicConfiguration::getActiveRecordDurationMonths)
                .orElse(ClinicConfiguration.DEFAULT_ACTIVE_RECORD_DURATION_MONTHS);

        Instant now = clockPort.now();
        Instant threshold = now.atZone(ZoneOffset.UTC).minusMonths(activeRecordDurationMonths).toInstant();

        if (visit.getStatus() != VisitStatus.COMPLETED || visit.getCompletedAt() == null || visit.getCompletedAt().isAfter(threshold)) {
            throw new MedicalRecordNotEligibleForArchiveException(
                    medicalRecordId,
                    "Hồ sơ bệnh án chưa quá thời hạn hoạt động (" + activeRecordDurationMonths + " tháng) tính từ khi kết thúc lượt khám."
            );
        }

        record.archive(userId, now);
        MedicalRecord saved = medicalRecordRepository.save(record);
        accessAuditService.recordRecordAccess(
                visit.getPatientId(), visit.getId(), saved.getId(), userId,
                MedicalRecordAccessAction.ARCHIVE, "Medical record archived", now);
        return resultMapper.toResult(saved, templateMapper.resolveApplied(saved, visit));
    }
}
