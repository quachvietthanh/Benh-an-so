package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.result.MedicalRecordResult;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class ArchiveMedicalRecordServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-21T00:00:00Z");
    private static final UUID RECORD_ID = UUID.randomUUID();
    private static final UUID VISIT_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();

    @Mock
    private MedicalRecordRepository medicalRecordRepository;
    @Mock
    private VisitRepository visitRepository;
    @Mock
    private MedicalRecordAuthorizationService authorizationService;
    @Mock
    private MedicalRecordAccessAuditService accessAuditService;
    @Mock
    private MedicalRecordResultMapper resultMapper;
    @Mock
    private ClockPort clockPort;

    @Test
    void archivesLockedRecordAndWritesAccessLog() {
        when(clockPort.now()).thenReturn(NOW);
        when(authorizationService.requireWriteAccess()).thenReturn(DOCTOR_ID);

        MedicalRecord record = MedicalRecord.restore(RECORD_ID, VISIT_ID, "c", "s", "h", "p", "cp", "tp", "di", "co",
                MedicalRecordStatus.LOCKED, NOW, DOCTOR_ID, DOCTOR_ID, NOW, null, null);
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));
        when(medicalRecordRepository.save(record)).thenReturn(record);

        Visit visit = Visit.restore(VISIT_ID, "V001", PATIENT_ID, DOCTOR_ID, null, null, VisitType.WALK_IN,
                VisitStatus.COMPLETED, NOW.minusSeconds(3600), NOW.minusSeconds(1800), NOW,
                "Checkup", null, DOCTOR_ID, NOW.minusSeconds(3600), NOW);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));

        when(resultMapper.toResult(record)).thenReturn(new MedicalRecordResult(
                RECORD_ID, VISIT_ID, "c", "s", "h", "p", "cp", "tp", "di", "co",
                MedicalRecordStatus.ARCHIVED, null, null, null, NOW, DOCTOR_ID, DOCTOR_ID, NOW, DOCTOR_ID, NOW));

        ArchiveMedicalRecordService service = new ArchiveMedicalRecordService(
                medicalRecordRepository, visitRepository, authorizationService,
                accessAuditService, resultMapper, clockPort);

        MedicalRecordResult result = service.archive(RECORD_ID);

        assertEquals(MedicalRecordStatus.ARCHIVED, result.status());
        assertEquals(MedicalRecordStatus.ARCHIVED, record.getStatus());
        verify(accessAuditService).recordRecordAccess(PATIENT_ID, VISIT_ID, RECORD_ID, DOCTOR_ID,
                MedicalRecordAccessAction.ARCHIVE, "Medical record archived", NOW);
    }
}
