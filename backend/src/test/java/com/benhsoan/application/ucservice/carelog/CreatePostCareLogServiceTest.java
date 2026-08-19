package com.benhsoan.application.ucservice.carelog;

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

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.carelog.PostCareLog;
import com.benhsoan.domain.carelog.enums.ContactChannel;
import com.benhsoan.domain.carelog.enums.ContactOutcome;
import com.benhsoan.domain.carelog.enums.PatientCondition;
import com.benhsoan.domain.followup.FollowUpReminder;
import com.benhsoan.domain.followup.enums.ReminderStatus;
import com.benhsoan.domain.followup.enums.ReminderType;
import com.benhsoan.domain.followup.exception.FollowUpReminderNotFoundException;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.carelog.CreatePostCareLogCommand;
import com.benhsoan.port.dto.result.PostCareLogResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.carelog.PostCareLogRepository;
import com.benhsoan.port.outbound.repository.followup.FollowUpReminderRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class CreatePostCareLogServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-15T08:00:00Z");
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID OTHER_PATIENT_ID = UUID.randomUUID();
    private static final UUID REMINDER_ID = UUID.randomUUID();

    private final PostCareLogRepository postCareLogRepository = mock(PostCareLogRepository.class);
    private final FollowUpReminderRepository followUpReminderRepository = mock(FollowUpReminderRepository.class);
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);
    private final PostCareLogAuthorizer authorizer = new PostCareLogAuthorizer(currentUserPort);
    private final PostCareLogResultMapper resultMapper = new PostCareLogResultMapper();

    private CreatePostCareLogService service;

    @BeforeEach
    void setUp() {
        service = new CreatePostCareLogService(
                postCareLogRepository, followUpReminderRepository, patientRepository,
                auditLogRepository, resultMapper, authorizer, currentUserPort, clockPort);

        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR);
        when(clockPort.now()).thenReturn(NOW);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(mock(Patient.class)));
        when(postCareLogRepository.save(any(PostCareLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsCareLogAndWritesAuditLog() {
        PostCareLogResult result = service.create(command(null, null, ContactOutcome.REACHED));

        assertEquals(PATIENT_ID, result.patientId());
        assertEquals(ACTOR, result.performedBy());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void transitionsReminderToSentWhenNotReached() {
        FollowUpReminder reminder = pendingReminderFor(PATIENT_ID);
        when(followUpReminderRepository.findById(REMINDER_ID)).thenReturn(Optional.of(reminder));
        when(followUpReminderRepository.save(any(FollowUpReminder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(command(REMINDER_ID, null, ContactOutcome.UNREACHABLE));

        assertEquals(ReminderStatus.SENT, reminder.getStatus());
    }

    @Test
    void rejectsUnknownPatient() {
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class,
                () -> service.create(command(null, null, ContactOutcome.REACHED)));

        verify(postCareLogRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownReminder() {
        when(followUpReminderRepository.findById(REMINDER_ID)).thenReturn(Optional.empty());

        assertThrows(FollowUpReminderNotFoundException.class,
                () -> service.create(command(REMINDER_ID, null, ContactOutcome.REACHED)));

        verify(postCareLogRepository, never()).save(any());
    }

    @Test
    void rejectsReminderOfAnotherPatient() {
        when(followUpReminderRepository.findById(REMINDER_ID))
                .thenReturn(Optional.of(pendingReminderFor(OTHER_PATIENT_ID)));

        assertThrows(ValidationException.class,
                () -> service.create(command(REMINDER_ID, null, ContactOutcome.REACHED)));

        verify(postCareLogRepository, never()).save(any());
    }

    @Test
    void rejectsMissingPatientId() {
        assertThrows(ValidationException.class, () -> service.create(
                new CreatePostCareLogCommand(null, null, null, ContactChannel.PHONE, NOW,
                        PatientCondition.STABLE, "notes", ContactOutcome.REACHED)));

        verify(postCareLogRepository, never()).save(any());
    }

    @Test
    void rejectsUnauthorizedRole() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.create(command(null, null, ContactOutcome.REACHED)));

        verify(postCareLogRepository, never()).save(any());
    }

    private CreatePostCareLogCommand command(UUID reminderId, UUID visitId, ContactOutcome outcome) {
        return new CreatePostCareLogCommand(
                PATIENT_ID, reminderId, visitId, ContactChannel.PHONE, NOW,
                PatientCondition.STABLE, "Benh nhan on dinh", outcome);
    }

    private FollowUpReminder pendingReminderFor(UUID patientId) {
        return FollowUpReminder.restore(
                REMINDER_ID, patientId, null, null,
                LocalDate.of(2026, 8, 30), NOW, ReminderType.GENERAL,
                ReminderStatus.PENDING, null, ACTOR, NOW);
    }
}
