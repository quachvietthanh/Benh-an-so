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
import com.benhsoan.domain.medicalrecord.MedicalRecordAmendment;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAmendmentRequiresCompletedVisitException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotLockedException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.command.medicalrecord.AmendMedicalRecordCommand;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordAmendmentRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class AmendMedicalRecordServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-20T02:00:00Z");

    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private MedicalRecordAmendmentRepository amendmentRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private MedicalRecordAuthorizationService authorizationService;
    @Mock private MedicalRecordAccessAuditService accessAuditService;
    @Mock private ClockPort clockPort;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private MedicalRecordAmendmentAuditWriter amendmentAuditWriter;
    @Spy private MedicalRecordResultMapper resultMapper = new MedicalRecordResultMapper();
    @InjectMocks private AmendMedicalRecordService service;

    @Test
    void amendsSignedRecordAndWritesAmendAndAccessAudits() {
        UUID recordId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MedicalRecord record = record(recordId, visitId, userId, MedicalRecordStatus.LOCKED);
        Visit visit = visit(visitId, patientId, userId, VisitStatus.COMPLETED);

        when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(clockPort.now()).thenReturn(NOW);
        when(amendmentRepository.save(any(MedicalRecordAmendment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.amend(recordId, new AmendMedicalRecordCommand("Correction", "Clarification"));

        assertEquals(recordId, result.medicalRecordId());
        verify(amendmentAuditWriter).writeAmended(userId, recordId, "Clarification", NOW);
        verify(accessAuditService).recordRecordAccess(patientId, visitId, recordId, userId,
                MedicalRecordAccessAction.AMEND, "Medical record amended", NOW);
    }

    @Test
    void rejectsAmendmentWithBlankReason() {
        UUID recordId = UUID.randomUUID();
        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record(recordId, UUID.randomUUID(), UUID.randomUUID(), MedicalRecordStatus.LOCKED)));

        assertThrows(ValidationException.class,
                () -> service.amend(recordId, new AmendMedicalRecordCommand("Correction", " ")));

        verifyNoInteractions(authorizationService, currentUserPort, amendmentRepository, accessAuditService, amendmentAuditWriter);
    }

    @Test
    void rejectsAmendmentForUnsignedDraftRecord() {
        UUID recordId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record(recordId, UUID.randomUUID(), userId, MedicalRecordStatus.DRAFT)));

        assertThrows(MedicalRecordNotLockedException.class,
                () -> service.amend(recordId, new AmendMedicalRecordCommand("Correction", "Clarification")));

        verifyNoInteractions(amendmentRepository, accessAuditService, amendmentAuditWriter, authorizationService);
    }

    @Test
    void rejectsAmendmentForArchivedRecord() {
        UUID recordId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record(recordId, UUID.randomUUID(), userId, MedicalRecordStatus.ARCHIVED)));

        assertThrows(MedicalRecordNotLockedException.class,
                () -> service.amend(recordId, new AmendMedicalRecordCommand("Correction", "Clarification")));

        verifyNoInteractions(amendmentRepository, accessAuditService, amendmentAuditWriter, authorizationService);
    }

    @Test
    void rejectsAmendmentByAdminAndWritesDenialAudit() {
        UUID recordId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID assignedDoctorId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        MedicalRecord record = record(recordId, visitId, assignedDoctorId, MedicalRecordStatus.LOCKED);
        Visit visit = visit(visitId, patientId, assignedDoctorId, VisitStatus.COMPLETED);

        when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(currentUserPort.getCurrentUserId()).thenReturn(adminId);
        when(authorizationService.requireAmendAccess(assignedDoctorId)).thenThrow(new MedicalRecordAccessDeniedException());
        when(clockPort.now()).thenReturn(NOW);

        assertThrows(MedicalRecordAccessDeniedException.class,
                () -> service.amend(recordId, new AmendMedicalRecordCommand("Correction", "Clarification")));

        verify(amendmentAuditWriter).writeDenied(adminId, recordId,
                "Medical record amendment denied: not the responsible doctor.", NOW);
        verifyNoInteractions(amendmentRepository, accessAuditService);
    }

    @Test
    void rejectsAmendmentForNonAssignedDoctorAndWritesDenialAudit() {
        UUID recordId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID assignedDoctorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        MedicalRecord record = record(recordId, visitId, assignedDoctorId, MedicalRecordStatus.LOCKED);
        Visit visit = visit(visitId, patientId, assignedDoctorId, VisitStatus.COMPLETED);

        when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        when(authorizationService.requireAmendAccess(assignedDoctorId)).thenThrow(new MedicalRecordAccessDeniedException());
        when(clockPort.now()).thenReturn(NOW);

        assertThrows(MedicalRecordAccessDeniedException.class,
                () -> service.amend(recordId, new AmendMedicalRecordCommand("Correction", "Clarification")));

        verify(amendmentAuditWriter).writeDenied(actorId, recordId,
                "Medical record amendment denied: not the responsible doctor.", NOW);
        verifyNoInteractions(amendmentRepository, accessAuditService);
    }

    @Test
    void rejectsAmendmentForCancelledVisit() {
        UUID recordId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MedicalRecord record = record(recordId, visitId, userId, MedicalRecordStatus.LOCKED);
        Visit visit = visit(visitId, UUID.randomUUID(), userId, VisitStatus.CANCELLED);

        when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

        assertThrows(MedicalRecordAmendmentRequiresCompletedVisitException.class,
                () -> service.amend(recordId, new AmendMedicalRecordCommand("Correction", "Clarification")));

        verifyNoInteractions(amendmentRepository, accessAuditService, amendmentAuditWriter, authorizationService);
    }

    private MedicalRecord record(UUID recordId, UUID visitId, UUID creatorId, MedicalRecordStatus status) {
        return MedicalRecord.restore(recordId, visitId, "c", "s", "h", "p", "cp", "tp", "di", "co",
                status, status == MedicalRecordStatus.LOCKED ? NOW : null, creatorId, creatorId, NOW, null, null);
    }

    private Visit visit(UUID visitId, UUID patientId, UUID doctorId, VisitStatus status) {
        return Visit.restore(visitId, "VIS-001", patientId, doctorId, null, null, VisitType.WALK_IN, status,
                NOW, NOW, NOW, "Consultation", null, doctorId, NOW, NOW);
    }
}
