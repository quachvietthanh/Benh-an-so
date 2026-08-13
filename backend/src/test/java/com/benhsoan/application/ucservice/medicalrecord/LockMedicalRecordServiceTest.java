package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordInvalidVisitException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class LockMedicalRecordServiceTest {

    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private MedicalRecordAuthorizationService authorizationService;
    @Mock private MedicalRecordAccessAuditService accessAuditService;
    @Mock private ClockPort clockPort;
    @Spy private MedicalRecordResultMapper resultMapper = new MedicalRecordResultMapper();
    @InjectMocks private LockMedicalRecordService service;

    @Test
    void locksRecordForActiveVisitAndWritesLockAudit() {
        UUID visitId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-20T02:00:00Z");
        MedicalRecord record = MedicalRecord.create(visitId, "Headache", null, null, null, null, null, null,
                "Stable", userId, now);
        Visit visit = Visit.restore(visitId, "VIS-001", patientId, UUID.randomUUID(), null, null,
                VisitType.WALK_IN, VisitStatus.IN_PROGRESS, now, now, null, "Consultation", null, userId, now, now);
        when(authorizationService.requireWriteAccess()).thenReturn(userId);
        when(medicalRecordRepository.findById(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(clockPort.now()).thenReturn(now);
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.lock(record.getId());

        assertEquals(record.getId(), result.id());
        verify(accessAuditService).recordRecordAccess(patientId, visitId, record.getId(), userId,
                MedicalRecordAccessAction.LOCK, "Medical record locked", now);
    }

    @Test
    void rejectsLockForCancelledVisit() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-20T02:00:00Z");
        MedicalRecord record = MedicalRecord.create(visitId, "Headache", null, null, null, null, null, null,
                "Stable", userId, now);
        Visit visit = Visit.restore(visitId, "VIS-001", UUID.randomUUID(), UUID.randomUUID(), null, null,
                VisitType.WALK_IN, VisitStatus.CANCELLED, now, null, null, "Consultation", null, userId, now, now);
        when(authorizationService.requireWriteAccess()).thenReturn(userId);
        when(medicalRecordRepository.findById(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

        assertThrows(MedicalRecordInvalidVisitException.class, () -> service.lock(record.getId()));

        verifyNoInteractions(accessAuditService, clockPort);
    }
}
