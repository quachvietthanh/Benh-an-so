package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotEligibleForArchiveException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotSignedException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.result.MedicalRecordResult;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class ArchiveMedicalRecordServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-21T00:00:00Z");
    private static final UUID RECORD_ID = UUID.randomUUID();
    private static final UUID VISIT_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();

    @Mock
    private MedicalRecordRepository medicalRecordRepository;
    @Mock
    private VisitRepository visitRepository;
    @Mock
    private ClinicConfigurationRepository clinicConfigurationRepository;
    @Mock
    private MedicalRecordAuthorizationService authorizationService;
    @Mock
    private MedicalRecordAccessAuditService accessAuditService;
    @Mock
    private MedicalRecordTemplateApplicationMapper templateMapper;
    @Mock
    private MedicalRecordResultMapper resultMapper;
    @Mock
    private ClockPort clockPort;
    @InjectMocks
    private ArchiveMedicalRecordService service;

    @Test
    void archivesLockedRecordPastActiveDurationAndWritesAccessLog() {
        when(clockPort.now()).thenReturn(NOW);
        when(authorizationService.requireArchiveManageAccess()).thenReturn(ADMIN_ID);
        when(clinicConfigurationRepository.find()).thenReturn(Optional.empty()); // defaults to 12 months

        MedicalRecord record = MedicalRecord.restore(RECORD_ID, VISIT_ID, "c", "s", "h", "p", "cp", "tp", "di", "co",
                MedicalRecordStatus.LOCKED, NOW, ADMIN_ID, ADMIN_ID, NOW, null, null);
        when(medicalRecordRepository.findByIdForUpdate(RECORD_ID)).thenReturn(Optional.of(record));
        when(medicalRecordRepository.save(record)).thenReturn(record);

        // Completed 13 months ago
        Instant completedAt = NOW.atZone(ZoneOffset.UTC).minusMonths(13).toInstant();
        Visit visit = Visit.restore(VISIT_ID, "V001", PATIENT_ID, ADMIN_ID, null, null, VisitType.WALK_IN,
                VisitStatus.COMPLETED, completedAt.minusSeconds(3600), completedAt.minusSeconds(1800), completedAt,
                "Checkup", null, ADMIN_ID, completedAt.minusSeconds(3600), completedAt);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));

        when(resultMapper.toResult(record, null)).thenReturn(new MedicalRecordResult(
                RECORD_ID, VISIT_ID, "c", "s", "h", "p", "cp", "tp", "di", "co",
                MedicalRecordStatus.ARCHIVED, null, null, null, NOW, ADMIN_ID, ADMIN_ID, NOW, ADMIN_ID, NOW));

        MedicalRecordResult result = service.archive(RECORD_ID);

        assertEquals(MedicalRecordStatus.ARCHIVED, result.status());
        assertEquals(MedicalRecordStatus.ARCHIVED, record.getStatus());
        verify(accessAuditService).recordRecordAccess(PATIENT_ID, VISIT_ID, RECORD_ID, ADMIN_ID,
                MedicalRecordAccessAction.ARCHIVE, "Medical record archived", NOW);
    }

    @Test
    void archivesSignedRecordPastActiveDuration() {
        when(clockPort.now()).thenReturn(NOW);
        when(authorizationService.requireArchiveManageAccess()).thenReturn(ADMIN_ID);
        when(clinicConfigurationRepository.find()).thenReturn(Optional.empty());

        MedicalRecord record = MedicalRecord.restore(RECORD_ID, VISIT_ID, "c", "s", "h", "p", "cp", "tp", "di", "co",
                MedicalRecordStatus.SIGNED, NOW, ADMIN_ID, ADMIN_ID, NOW, null, null);
        when(medicalRecordRepository.findByIdForUpdate(RECORD_ID)).thenReturn(Optional.of(record));
        when(medicalRecordRepository.save(record)).thenReturn(record);

        Instant completedAt = NOW.atZone(ZoneOffset.UTC).minusMonths(14).toInstant();
        Visit visit = Visit.restore(VISIT_ID, "V001", PATIENT_ID, ADMIN_ID, null, null, VisitType.WALK_IN,
                VisitStatus.COMPLETED, completedAt.minusSeconds(3600), completedAt.minusSeconds(1800), completedAt,
                "Checkup", null, ADMIN_ID, completedAt.minusSeconds(3600), completedAt);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));

        when(resultMapper.toResult(record, null)).thenReturn(new MedicalRecordResult(
                RECORD_ID, VISIT_ID, "c", "s", "h", "p", "cp", "tp", "di", "co",
                MedicalRecordStatus.ARCHIVED, null, null, null, NOW, ADMIN_ID, ADMIN_ID, NOW, ADMIN_ID, NOW));

        MedicalRecordResult result = service.archive(RECORD_ID);

        assertEquals(MedicalRecordStatus.ARCHIVED, result.status());
        assertEquals(MedicalRecordStatus.ARCHIVED, record.getStatus());
    }

    @Test
    void throwsExceptionWhenRecordIsNotPastActiveDuration() {
        when(clockPort.now()).thenReturn(NOW);
        when(authorizationService.requireArchiveManageAccess()).thenReturn(ADMIN_ID);
        when(clinicConfigurationRepository.find()).thenReturn(Optional.empty());

        MedicalRecord record = MedicalRecord.restore(RECORD_ID, VISIT_ID, "c", "s", "h", "p", "cp", "tp", "di", "co",
                MedicalRecordStatus.SIGNED, NOW, ADMIN_ID, ADMIN_ID, NOW, null, null);
        when(medicalRecordRepository.findByIdForUpdate(RECORD_ID)).thenReturn(Optional.of(record));

        // Completed only 2 months ago (default active duration is 12 months)
        Instant completedAt = NOW.atZone(ZoneOffset.UTC).minusMonths(2).toInstant();
        Visit visit = Visit.restore(VISIT_ID, "V001", PATIENT_ID, ADMIN_ID, null, null, VisitType.WALK_IN,
                VisitStatus.COMPLETED, completedAt.minusSeconds(3600), completedAt.minusSeconds(1800), completedAt,
                "Checkup", null, ADMIN_ID, completedAt.minusSeconds(3600), completedAt);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));

        assertThrows(MedicalRecordNotEligibleForArchiveException.class, () -> service.archive(RECORD_ID));
    }

    @Test
    void throwsExceptionWhenRecordIsNotSignedAccordingToQTN41() {
        when(clockPort.now()).thenReturn(NOW);
        when(authorizationService.requireArchiveManageAccess()).thenReturn(ADMIN_ID);
        when(clinicConfigurationRepository.find()).thenReturn(Optional.empty());

        MedicalRecord record = MedicalRecord.restore(RECORD_ID, VISIT_ID, "c", "s", "h", "p", "cp", "tp", "di", "co",
                MedicalRecordStatus.OPEN, null, null, ADMIN_ID, NOW, null, null);
        when(medicalRecordRepository.findByIdForUpdate(RECORD_ID)).thenReturn(Optional.of(record));

        Instant completedAt = NOW.atZone(ZoneOffset.UTC).minusMonths(15).toInstant();
        Visit visit = Visit.restore(VISIT_ID, "V001", PATIENT_ID, ADMIN_ID, null, null, VisitType.WALK_IN,
                VisitStatus.COMPLETED, completedAt.minusSeconds(3600), completedAt.minusSeconds(1800), completedAt,
                "Checkup", null, ADMIN_ID, completedAt.minusSeconds(3600), completedAt);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));

        assertThrows(MedicalRecordNotSignedException.class, () -> service.archive(RECORD_ID));
    }
}
