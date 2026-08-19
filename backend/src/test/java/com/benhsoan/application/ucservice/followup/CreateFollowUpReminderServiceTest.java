package com.benhsoan.application.ucservice.followup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.followup.FollowUpReminder;
import com.benhsoan.domain.followup.enums.ReminderStatus;
import com.benhsoan.domain.followup.enums.ReminderType;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.followup.CreateFollowUpReminderCommand;
import com.benhsoan.port.dto.result.FollowUpReminderResult;
import com.benhsoan.port.outbound.repository.followup.FollowUpReminderRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class CreateFollowUpReminderServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-15T08:00:00Z");
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID OTHER_PATIENT_ID = UUID.randomUUID();
    private static final UUID VISIT_ID = UUID.randomUUID();

    private final FollowUpReminderRepository repository = mock(FollowUpReminderRepository.class);
    private final VisitRepository visitRepository = mock(VisitRepository.class);
    private final MedicalRecordRepository medicalRecordRepository = mock(MedicalRecordRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);
    private final FollowUpReminderAuthorizer authorizer = new FollowUpReminderAuthorizer(currentUserPort);
    private final FollowUpReminderResultMapper resultMapper = new FollowUpReminderResultMapper();

    private CreateFollowUpReminderService service;

    @BeforeEach
    void setUp() {
        service = new CreateFollowUpReminderService(
                repository, visitRepository, medicalRecordRepository, resultMapper,
                authorizer, currentUserPort, clockPort);

        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(false);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR);
        when(clockPort.now()).thenReturn(NOW);
        when(repository.save(any(FollowUpReminder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsReminderWithPendingStatus() {
        stubVisitAndIndication();

        FollowUpReminderResult result = service.create(new CreateFollowUpReminderCommand(
                PATIENT_ID, VISIT_ID, null, LocalDate.of(2026, 8, 30), NOW, ReminderType.REVISIT, "Recheck"));

        assertEquals(ReminderStatus.PENDING, result.status());
        assertEquals(PATIENT_ID, result.patientId());
        assertEquals(ACTOR, result.createdBy());
        verify(repository).save(any(FollowUpReminder.class));
    }

    @Test
    void defaultsReminderTypeToGeneralWhenMissing() {
        stubVisitAndIndication();

        FollowUpReminderResult result = service.create(new CreateFollowUpReminderCommand(
                PATIENT_ID, VISIT_ID, null, LocalDate.of(2026, 8, 30), NOW, null, null));

        assertEquals(ReminderType.GENERAL, result.reminderType());
    }
    @Test
    void rejectsMissingVisitId() {
        assertThrows(ValidationException.class, () -> service.create(new CreateFollowUpReminderCommand(
                PATIENT_ID, null, null, LocalDate.of(2026, 8, 30), NOW, ReminderType.GENERAL, null)));

        verify(repository, never()).save(any());
    }

    @Test
    void rejectsUnknownVisit() {
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.empty());

        assertThrows(VisitNotFoundException.class, () -> service.create(new CreateFollowUpReminderCommand(
                PATIENT_ID, VISIT_ID, null, LocalDate.of(2026, 8, 30), NOW, ReminderType.GENERAL, null)));
    }

    @Test
    void rejectsVisitOfDifferentPatient() {
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visitFor(OTHER_PATIENT_ID)));

        assertThrows(ValidationException.class, () -> service.create(new CreateFollowUpReminderCommand(
                PATIENT_ID, VISIT_ID, null, LocalDate.of(2026, 8, 30), NOW, ReminderType.GENERAL, null)));

        verify(repository, never()).save(any());
    }

    @Test
    void rejectsVisitWithoutDoctorFollowUpIndication() {
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visitFor(PATIENT_ID)));
        when(medicalRecordRepository.findByVisitId(VISIT_ID))
                .thenReturn(Optional.of(recordWithInstructions("   ")));

        assertThrows(ValidationException.class, () -> service.create(new CreateFollowUpReminderCommand(
                PATIENT_ID, VISIT_ID, null, LocalDate.of(2026, 8, 30), NOW, ReminderType.GENERAL, null)));

        verify(repository, never()).save(any());
    }

    @Test
    void rejectsVisitWithoutMedicalRecord() {
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visitFor(PATIENT_ID)));
        when(medicalRecordRepository.findByVisitId(VISIT_ID)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> service.create(new CreateFollowUpReminderCommand(
                PATIENT_ID, VISIT_ID, null, LocalDate.of(2026, 8, 30), NOW, ReminderType.GENERAL, null)));
    }

    @Test
    void rejectsMissingFollowUpDate() {
        stubVisitAndIndication();

        assertThrows(ValidationException.class, () -> service.create(new CreateFollowUpReminderCommand(
                PATIENT_ID, VISIT_ID, null, null, NOW, ReminderType.GENERAL, null)));

        verify(repository, never()).save(any());
    }

    @Test
    void rejectsNonAuthorizedRole() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.create(new CreateFollowUpReminderCommand(
                PATIENT_ID, VISIT_ID, null, LocalDate.of(2026, 8, 30), NOW, ReminderType.GENERAL, null)));

        verify(repository, never()).save(any());
    }

    private void stubVisitAndIndication() {
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visitFor(PATIENT_ID)));
        when(medicalRecordRepository.findByVisitId(VISIT_ID))
                .thenReturn(Optional.of(recordWithInstructions("Tái khám sau 5 ngày")));
    }

    private Visit visitFor(UUID patientId) {
        return Visit.restore(
                VISIT_ID,
                "VIS-001",
                patientId,
                UUID.randomUUID(),
                null,
                null,
                VisitType.WALK_IN,
                VisitStatus.COMPLETED,
                Instant.parse("2026-08-10T09:00:00Z"),
                Instant.parse("2026-08-10T09:30:00Z"),
                Instant.parse("2026-08-10T10:00:00Z"),
                "Kham tong quat",
                null,
                ACTOR,
                Instant.parse("2026-08-10T09:00:00Z"),
                null
        );
    }

    private MedicalRecord recordWithInstructions(String doctorInstructions) {
        return MedicalRecord.restore(
                UUID.randomUUID(),
                VISIT_ID,
                "Dau dau",
                "Dau dau nhe",
                "Khong co benh nen",
                "Huyet ap on dinh",
                "Dang theo doi",
                "Nghi ngoi va theo doi",
                doctorInstructions,
                "Chua co dau hieu bat thuong",
                MedicalRecordStatus.LOCKED,
                null,
                null,
                ACTOR,
                Instant.parse("2026-08-10T10:00:00Z"),
                null,
                null
        );
    }
}

