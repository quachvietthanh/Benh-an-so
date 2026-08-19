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
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.followup.FollowUpReminder;
import com.benhsoan.domain.followup.enums.ReminderStatus;
import com.benhsoan.domain.followup.enums.ReminderType;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.followup.CreateFollowUpReminderCommand;
import com.benhsoan.port.dto.result.FollowUpReminderResult;
import com.benhsoan.port.outbound.repository.followup.FollowUpReminderRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class CreateFollowUpReminderServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-15T08:00:00Z");
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();

    private final FollowUpReminderRepository repository = mock(FollowUpReminderRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);
    private final FollowUpReminderAuthorizer authorizer = new FollowUpReminderAuthorizer(currentUserPort);
    private final FollowUpReminderResultMapper resultMapper = new FollowUpReminderResultMapper();

    private CreateFollowUpReminderService service;

    @BeforeEach
    void setUp() {
        service = new CreateFollowUpReminderService(
                repository, resultMapper, authorizer, currentUserPort, clockPort);

        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(false);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR);
        when(clockPort.now()).thenReturn(NOW);
        when(repository.save(any(FollowUpReminder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsReminderWithPendingStatus() {
        FollowUpReminderResult result = service.create(new CreateFollowUpReminderCommand(
                PATIENT_ID, null, null, LocalDate.of(2026, 8, 30), NOW, ReminderType.REVISIT, "Recheck"));

        assertEquals(ReminderStatus.PENDING, result.status());
        assertEquals(PATIENT_ID, result.patientId());
        assertEquals(ACTOR, result.createdBy());
        verify(repository).save(any(FollowUpReminder.class));
    }

    @Test
    void defaultsReminderTypeToGeneralWhenMissing() {
        FollowUpReminderResult result = service.create(new CreateFollowUpReminderCommand(
                PATIENT_ID, null, null, LocalDate.of(2026, 8, 30), NOW, null, null));

        assertEquals(ReminderType.GENERAL, result.reminderType());
    }

    @Test
    void rejectsMissingFollowUpDate() {
        assertThrows(ValidationException.class, () -> service.create(new CreateFollowUpReminderCommand(
                PATIENT_ID, null, null, null, NOW, ReminderType.GENERAL, null)));

        verify(repository, never()).save(any());
    }

    @Test
    void rejectsNonAuthorizedRole() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.create(new CreateFollowUpReminderCommand(
                PATIENT_ID, null, null, LocalDate.of(2026, 8, 30), NOW, ReminderType.GENERAL, null)));

        verify(repository, never()).save(any());
    }
}
