package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordInvalidVisitException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.command.medicalrecord.UpdateMedicalRecordCommand;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class UpdateMedicalRecordServiceTest {

    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private MedicalRecordAuthorizationService authorizationService;
    @Mock private MedicalRecordAccessAuditService accessAuditService;
    @Mock private MedicalRecordTemplateApplicationMapper templateMapper;
    @Mock private ClockPort clockPort;
    @Spy private MedicalRecordResultMapper resultMapper = new MedicalRecordResultMapper();
    @InjectMocks private UpdateMedicalRecordService service;

    @Test
    void updatesRecordForActiveVisitAndWritesUpdateAudit() {
        UUID visitId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-20T02:00:00Z");
        MedicalRecord record = MedicalRecord.create(visitId, "Headache", null, null, null, null, null, null,
                "Stable", userId, now);
        Visit visit = Visit.restore(visitId, "VIS-001", patientId, UUID.randomUUID(), null, null,
                VisitType.WALK_IN, VisitStatus.IN_PROGRESS, now, now, null, "Consultation", null, userId, now, now);
        when(authorizationService.requireContentWriteAccess(record.getId())).thenReturn(userId);
        when(medicalRecordRepository.findByIdForUpdate(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(clockPort.now()).thenReturn(now);
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.update(record.getId(), new UpdateMedicalRecordCommand("Updated complaint", null, null, null,
                null, null, null, "Updated conclusion"));

        assertEquals(record.getId(), result.id());
        verify(authorizationService).requireContentVisitWriteAccess(userId, visit.getDoctorId(), record.getId());
        verify(medicalRecordRepository).findByIdForUpdate(record.getId());
        verify(templateMapper).resolveApplied(record, visit);
        verify(accessAuditService).recordRecordAccess(patientId, visitId, record.getId(), userId,
                MedicalRecordAccessAction.UPDATE, "Medical record updated", now);
    }

    @Test
    void rejectsUpdateWhenDoctorIsNotAttendingDoctor() {
        UUID visitId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID otherDoctorId = UUID.randomUUID();
        UUID attendingDoctorId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-20T02:00:00Z");
        MedicalRecord record = MedicalRecord.create(visitId, "Headache", null, null, null, null, null, null,
                "Stable", attendingDoctorId, now);
        Visit visit = Visit.restore(visitId, "VIS-001", patientId, attendingDoctorId, null, null,
                VisitType.WALK_IN, VisitStatus.IN_PROGRESS, now, now, null, "Consultation", null, attendingDoctorId, now, now);

        when(authorizationService.requireContentWriteAccess(record.getId())).thenReturn(otherDoctorId);
        when(medicalRecordRepository.findByIdForUpdate(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        doThrow(new MedicalRecordAccessDeniedException()).when(authorizationService)
                .requireContentVisitWriteAccess(otherDoctorId, attendingDoctorId, record.getId());

        assertThrows(MedicalRecordAccessDeniedException.class,
                () -> service.update(record.getId(), new UpdateMedicalRecordCommand("Headache", null, null, null,
                        null, null, null, "Stable")));

        verifyNoInteractions(accessAuditService, clockPort);
    }

    @Test
    void rejectsDirectUpdateForCompletedVisit() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-20T02:00:00Z");
        MedicalRecord record = MedicalRecord.create(visitId, "Headache", null, null, null, null, null, null,
                "Stable", userId, now);
        Visit visit = Visit.restore(visitId, "VIS-001", UUID.randomUUID(), UUID.randomUUID(), null, null,
                VisitType.WALK_IN, VisitStatus.COMPLETED, now, now, now, "Consultation", null, userId, now, now);
        when(authorizationService.requireContentWriteAccess(record.getId())).thenReturn(userId);
        when(medicalRecordRepository.findByIdForUpdate(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

        assertThrows(MedicalRecordInvalidVisitException.class,
                () -> service.update(record.getId(), new UpdateMedicalRecordCommand("Headache", null, null, null,
                        null, null, null, "Stable")));

        verifyNoInteractions(accessAuditService, clockPort);
    }
}
