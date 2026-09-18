package com.benhsoan.application.ucservice.visit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.benhsoan.application.ucservice.medicalrecord.MedicalRecordAccessAuditService;
import com.benhsoan.application.ucservice.medicalrecord.MedicalRecordAuthorizationAuditService;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.constant.RoleConstants;
import com.benhsoan.domain.auth.exception.UserNotFoundException;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAlreadyLockedException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.VisitHandover;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.domain.visit.exception.VisitEncounterAccessDeniedException;
import com.benhsoan.port.dto.command.visit.HandoverPatientCommand;
import com.benhsoan.port.dto.result.VisitHandoverResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitHandoverRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class HandoverPatientServiceTest {

    private CurrentUserPort currentUserPort;
    private VisitRepository visitRepository;
    private VisitHandoverRepository visitHandoverRepository;
    private UserRepository userRepository;
    private MedicalRecordRepository medicalRecordRepository;
    private MedicalRecordAccessAuditService medicalRecordAccessAuditService;
    private MedicalRecordAuthorizationAuditService medicalRecordAuthorizationAuditService;
    private AuditLogRepository auditLogRepository;
    private ClockPort clockPort;

    private HandoverPatientService service;

    private final UUID doctorAId = UUID.randomUUID();
    private final UUID doctorBId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-17T10:00:00Z");

    @BeforeEach
    void setUp() {
        currentUserPort = mock(CurrentUserPort.class);
        visitRepository = mock(VisitRepository.class);
        visitHandoverRepository = mock(VisitHandoverRepository.class);
        userRepository = mock(UserRepository.class);
        medicalRecordRepository = mock(MedicalRecordRepository.class);
        medicalRecordAccessAuditService = mock(MedicalRecordAccessAuditService.class);
        medicalRecordAuthorizationAuditService = mock(MedicalRecordAuthorizationAuditService.class);
        auditLogRepository = mock(AuditLogRepository.class);
        clockPort = mock(ClockPort.class);

        when(clockPort.now()).thenReturn(now);

        service = new HandoverPatientService(
                currentUserPort,
                visitRepository,
                visitHandoverRepository,
                userRepository,
                medicalRecordRepository,
                medicalRecordAccessAuditService,
                medicalRecordAuthorizationAuditService,
                auditLogRepository,
                clockPort
        );
    }

    private Visit createActiveVisit() {
        Visit v = Visit.create("VIS001", patientId, doctorAId, null, null, VisitType.WALK_IN, now, "Kham dau dau", null, doctorAId);
        v.start(now);
        return v;
    }

    @Test
    void handoverSuccessByOwningDoctor() {
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorAId);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);

        Visit visit = createActiveVisit();
        UUID visitId = visit.getId();
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));

        User doctorB = User.restore(doctorBId, "doctorB", "pass", "Dr. Bob", "bob@example.com", "0900000002",
                RoleConstants.DOCTOR, true, null, now);
        when(userRepository.findById(doctorBId)).thenReturn(Optional.of(doctorB));

        User doctorA = User.restore(doctorAId, "doctorA", "pass", "Dr. Alice", "alice@example.com", "0900000001",
                RoleConstants.DOCTOR, true, null, now);
        when(userRepository.findById(doctorAId)).thenReturn(Optional.of(doctorA));

        UUID recordId = UUID.randomUUID();
        MedicalRecord record = mock(MedicalRecord.class);
        when(record.getId()).thenReturn(recordId);
        when(record.isContentLocked()).thenReturn(false);
        when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.of(record));

        when(visitHandoverRepository.save(any(VisitHandover.class))).thenAnswer(inv -> inv.getArgument(0));

        HandoverPatientCommand command = new HandoverPatientCommand(doctorBId, "Can y kien chuyen khoa than kinh");
        VisitHandoverResult result = service.handover(visitId, command);

        assertNotNull(result);
        assertEquals(doctorAId, result.fromDoctorId());
        assertEquals("Dr. Alice", result.fromDoctorName());
        assertEquals(doctorBId, result.toDoctorId());
        assertEquals("Dr. Bob", result.toDoctorName());
        assertEquals("Can y kien chuyen khoa than kinh", result.reason());
        assertEquals(now, result.handedOverAt());

        assertEquals(doctorBId, visit.getDoctorId());
        assertEquals(doctorAId, visit.getInitialDoctorId());

        verify(visitRepository).save(visit);
        verify(visitHandoverRepository).save(any(VisitHandover.class));
        verify(auditLogRepository).save(any(AuditLog.class));
        verify(medicalRecordAccessAuditService).recordRecordAccess(
                eq(patientId), eq(visitId), eq(recordId), eq(doctorAId),
                eq(MedicalRecordAccessAction.HANDOVER), any(), eq(now)
        );
    }

    @Test
    void handoverSuccessByAdmin() {
        UUID adminId = UUID.randomUUID();
        when(currentUserPort.getCurrentUserId()).thenReturn(adminId);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);

        Visit visit = createActiveVisit();
        UUID visitId = visit.getId();
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));

        User doctorB = User.restore(doctorBId, "doctorB", "pass", "Dr. Bob", "bob@example.com", "0900000002",
                RoleConstants.DOCTOR, true, null, now);
        when(userRepository.findById(doctorBId)).thenReturn(Optional.of(doctorB));
        when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.empty());
        when(visitHandoverRepository.save(any(VisitHandover.class))).thenAnswer(inv -> inv.getArgument(0));

        HandoverPatientCommand command = new HandoverPatientCommand(doctorBId, "Dieu dong dot xuat");
        VisitHandoverResult result = service.handover(visitId, command);

        assertEquals(doctorBId, result.toDoctorId());
        assertEquals(doctorBId, visit.getDoctorId());
        assertEquals(doctorAId, visit.getInitialDoctorId());
    }

    @Test
    void rejectsWhenActorNotOwningDoctorNorAdmin() {
        UUID doctorCId = UUID.randomUUID();
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorCId);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);

        Visit visit = createActiveVisit();
        UUID visitId = visit.getId();
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));

        HandoverPatientCommand command = new HandoverPatientCommand(doctorBId, "Ly do");
        assertThrows(VisitEncounterAccessDeniedException.class, () -> service.handover(visitId, command));
        verify(medicalRecordAuthorizationAuditService).recordHandoverAccessDenied(eq(doctorCId), eq(visitId), any());
    }

    @Test
    void rejectsWhenVisitAlreadyCompleted() {
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorAId);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);

        Visit visit = createActiveVisit();
        visit.complete(now.plusSeconds(100));
        UUID visitId = visit.getId();
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));

        HandoverPatientCommand command = new HandoverPatientCommand(doctorBId, "Ly do");
        assertThrows(ValidationException.class, () -> service.handover(visitId, command));
    }

    @Test
    void rejectsWhenTargetDoctorIsSameAsCurrentDoctor() {
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorAId);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);

        Visit visit = createActiveVisit();
        UUID visitId = visit.getId();
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));

        HandoverPatientCommand command = new HandoverPatientCommand(doctorAId, "Ly do");
        assertThrows(ValidationException.class, () -> service.handover(visitId, command));
    }

    @Test
    void rejectsWhenTargetDoctorNotFound() {
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorAId);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);

        Visit visit = createActiveVisit();
        UUID visitId = visit.getId();
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(userRepository.findById(doctorBId)).thenReturn(Optional.empty());

        HandoverPatientCommand command = new HandoverPatientCommand(doctorBId, "Ly do");
        assertThrows(UserNotFoundException.class, () -> service.handover(visitId, command));
    }

    @Test
    void rejectsWhenTargetUserIsNotDoctorOrInactive() {
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorAId);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);

        Visit visit = createActiveVisit();
        UUID visitId = visit.getId();
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));

        UUID receptionistId = UUID.randomUUID();
        User notDoctor = User.restore(receptionistId, "recept", "pass", "Recept", "r@example.com", "0900000003",
                UUID.randomUUID(), true, null, now);
        when(userRepository.findById(receptionistId)).thenReturn(Optional.of(notDoctor));

        HandoverPatientCommand command = new HandoverPatientCommand(receptionistId, "Ly do");
        assertThrows(ValidationException.class, () -> service.handover(visitId, command));
    }

    @Test
    void rejectsWhenMedicalRecordAlreadySigned() {
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorAId);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);

        Visit visit = createActiveVisit();
        UUID visitId = visit.getId();
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));

        User doctorB = User.restore(doctorBId, "doctorB", "pass", "Dr. Bob", "bob@example.com", "0900000002",
                RoleConstants.DOCTOR, true, null, now);
        when(userRepository.findById(doctorBId)).thenReturn(Optional.of(doctorB));

        MedicalRecord lockedRecord = mock(MedicalRecord.class);
        when(lockedRecord.isContentLocked()).thenReturn(true);
        when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.of(lockedRecord));

        HandoverPatientCommand command = new HandoverPatientCommand(doctorBId, "Ly do");
        assertThrows(MedicalRecordAlreadyLockedException.class, () -> service.handover(visitId, command));

        verify(visitRepository, never()).save(any());
        verify(visitHandoverRepository, never()).save(any());
    }

    @Test
    void rejectsWhenReasonIsBlank() {
        HandoverPatientCommand command = new HandoverPatientCommand(doctorBId, "   ");
        assertThrows(ValidationException.class, () -> service.handover(UUID.randomUUID(), command));
    }
}
