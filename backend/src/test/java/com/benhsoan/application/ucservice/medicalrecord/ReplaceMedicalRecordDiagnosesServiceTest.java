package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.enums.DiagnosisType;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAlreadyLockedException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordInvalidVisitException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.command.medicalrecord.ReplaceMedicalRecordDiagnosesCommand;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class ReplaceMedicalRecordDiagnosesServiceTest {

    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private MedicalRecordDiagnosisRepository medicalRecordDiagnosisRepository;
    @Mock private DiagnosisCatalogRepository diagnosisCatalogRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private MedicalRecordAuthorizationService authorizationService;
    @Mock private MedicalRecordAccessAuditService accessAuditService;
    @Mock private ClockPort clockPort;
    @Spy private MedicalRecordDiagnosisResultMapper resultMapper = new MedicalRecordDiagnosisResultMapper();
    @InjectMocks private ReplaceMedicalRecordDiagnosesService service;

    @Test
    void persistsPrimaryAndSecondaryDiagnosesAndWritesSafeAuditDetail() {
        UUID actorId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID primaryCatalogId = UUID.randomUUID();
        UUID secondaryCatalogId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-20T02:00:00Z");
        MedicalRecord record = MedicalRecord.create(visitId, null, null, null, null, null, null, null, null, actorId, now);
        Visit visit = Visit.restore(visitId, "VIS-001", patientId, UUID.randomUUID(), null, null, VisitType.WALK_IN,
                VisitStatus.IN_PROGRESS, now, now, null, "Exam", null, actorId, now, null);
        DiagnosisCatalog primary = DiagnosisCatalog.restore(primaryCatalogId, "J06.9", "Upper respiratory infection", null, true, now, null);
        DiagnosisCatalog secondary = DiagnosisCatalog.restore(secondaryCatalogId, "R50.9", "Fever", null, true, now, null);
        when(authorizationService.requireWriteAccess()).thenReturn(actorId);
        when(medicalRecordRepository.findById(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(clockPort.now()).thenReturn(now);
        when(diagnosisCatalogRepository.findById(primaryCatalogId)).thenReturn(Optional.of(primary));
        when(diagnosisCatalogRepository.findById(secondaryCatalogId)).thenReturn(Optional.of(secondary));
        when(medicalRecordDiagnosisRepository.replaceForMedicalRecord(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

        var result = service.replace(record.getId(), command(primaryCatalogId, "J06.9", "Upper respiratory infection", secondaryCatalogId, "R50.9", "Fever"));

        assertEquals(2, result.size());
        assertEquals(DiagnosisType.PRIMARY, result.getFirst().diagnosisType());
        ArgumentCaptor<String> detailCaptor = ArgumentCaptor.forClass(String.class);
        verify(accessAuditService).recordRecordAccess(org.mockito.ArgumentMatchers.eq(patientId), org.mockito.ArgumentMatchers.eq(visitId),
                org.mockito.ArgumentMatchers.eq(record.getId()), org.mockito.ArgumentMatchers.eq(actorId),
                org.mockito.ArgumentMatchers.eq(MedicalRecordAccessAction.UPDATE), detailCaptor.capture(), org.mockito.ArgumentMatchers.eq(now));
        assertEquals("Medical record diagnoses replaced", detailCaptor.getValue());
    }

    @Test
    void rejectsDuplicatesBeforeReplacingExistingDiagnoses() {
        UUID actorId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-20T02:00:00Z");
        MedicalRecord record = MedicalRecord.create(visitId, null, null, null, null, null, null, null, null, actorId, now);
        Visit visit = Visit.restore(visitId, "VIS-001", UUID.randomUUID(), UUID.randomUUID(), null, null, VisitType.WALK_IN,
                VisitStatus.IN_PROGRESS, now, now, null, "Exam", null, actorId, now, null);
        UUID catalogId = UUID.randomUUID();
        when(authorizationService.requireWriteAccess()).thenReturn(actorId);
        when(medicalRecordRepository.findById(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

        assertThrows(ValidationException.class, () -> service.replace(record.getId(),
                command(catalogId, "J06.9", "Upper respiratory infection", catalogId, "J06.9", "Upper respiratory infection")));

        verify(medicalRecordDiagnosisRepository, never()).replaceForMedicalRecord(any(), any());
    }

    @Test
    void rejectsLockedRecordBeforeReplacingExistingDiagnoses() {
        UUID actorId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-20T02:00:00Z");
        MedicalRecord record = MedicalRecord.create(visitId, "Complaint", null, null, null, null, null, null, "Conclusion", actorId, now);
        record.lock(actorId, now);
        when(authorizationService.requireWriteAccess()).thenReturn(actorId);
        when(medicalRecordRepository.findById(record.getId())).thenReturn(Optional.of(record));

        assertThrows(MedicalRecordAlreadyLockedException.class, () -> service.replace(record.getId(),
                command(UUID.randomUUID(), "J06.9", "Upper respiratory infection", UUID.randomUUID(), "R50.9", "Fever")));

        verify(medicalRecordDiagnosisRepository, never()).replaceForMedicalRecord(any(), any());
    }

    @Test
    void rejectsCompletedVisitBeforeReplacingExistingDiagnoses() {
        UUID actorId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-20T02:00:00Z");
        MedicalRecord record = MedicalRecord.create(visitId, null, null, null, null, null, null, null, null, actorId, now);
        Visit visit = Visit.restore(visitId, "VIS-001", UUID.randomUUID(), UUID.randomUUID(), null, null, VisitType.WALK_IN,
                VisitStatus.COMPLETED, now, now, now, "Exam", null, actorId, now, now);
        when(authorizationService.requireWriteAccess()).thenReturn(actorId);
        when(medicalRecordRepository.findById(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

        assertThrows(MedicalRecordInvalidVisitException.class, () -> service.replace(record.getId(),
                command(UUID.randomUUID(), "J06.9", "Upper respiratory infection", UUID.randomUUID(), "R50.9", "Fever")));

        verify(medicalRecordDiagnosisRepository, never()).replaceForMedicalRecord(any(), any());
    }

    @Test
    void doesNotWriteAuditWhenReplacingDiagnosesFails() {
        UUID actorId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID catalogId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-20T02:00:00Z");
        MedicalRecord record = MedicalRecord.create(visitId, null, null, null, null, null, null, null, null, actorId, now);
        Visit visit = Visit.restore(visitId, "VIS-001", UUID.randomUUID(), UUID.randomUUID(), null, null, VisitType.WALK_IN,
                VisitStatus.IN_PROGRESS, now, now, null, "Exam", null, actorId, now, null);
        DiagnosisCatalog catalog = DiagnosisCatalog.restore(catalogId, "J06.9", "Upper respiratory infection", null, true, now, null);
        when(authorizationService.requireWriteAccess()).thenReturn(actorId);
        when(medicalRecordRepository.findById(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(clockPort.now()).thenReturn(now);
        when(diagnosisCatalogRepository.findById(catalogId)).thenReturn(Optional.of(catalog));
        doThrow(new IllegalStateException("Database write failed"))
                .when(medicalRecordDiagnosisRepository).replaceForMedicalRecord(any(), any());

        assertThrows(IllegalStateException.class, () -> service.replace(record.getId(),
                new ReplaceMedicalRecordDiagnosesCommand(
                        new ReplaceMedicalRecordDiagnosesCommand.DiagnosisCommand(catalogId, "J06.9", "Upper respiratory infection", null),
                        List.of()
                )));

        verify(accessAuditService, never()).recordRecordAccess(any(), any(), any(), any(), any(), any(), any());
    }

    private ReplaceMedicalRecordDiagnosesCommand command(UUID primaryCatalogId, String primaryCode, String primaryName,
                                                          UUID secondaryCatalogId, String secondaryCode, String secondaryName) {
        return new ReplaceMedicalRecordDiagnosesCommand(
                new ReplaceMedicalRecordDiagnosesCommand.DiagnosisCommand(primaryCatalogId, primaryCode, primaryName, "Sensitive note"),
                List.of(new ReplaceMedicalRecordDiagnosesCommand.DiagnosisCommand(secondaryCatalogId, secondaryCode, secondaryName, null))
        );
    }
}
