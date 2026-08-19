package com.benhsoan.application.ucservice.followup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import com.benhsoan.domain.followup.exception.FollowUpReminderNotFoundException;
import com.benhsoan.port.dto.result.FollowUpReminderResult;
import com.benhsoan.port.outbound.repository.followup.FollowUpReminderRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

class UpdateFollowUpReminderStatusServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-15T08:00:00Z");
    private static final UUID REMINDER_ID = UUID.randomUUID();

    private final FollowUpReminderRepository repository = mock(FollowUpReminderRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final FollowUpReminderAuthorizer authorizer = new FollowUpReminderAuthorizer(currentUserPort);
    private final FollowUpReminderResultMapper resultMapper = new FollowUpReminderResultMapper();

    private UpdateFollowUpReminderStatusService service;

    @BeforeEach
    void setUp() {
        service = new UpdateFollowUpReminderStatusService(repository, resultMapper, authorizer);

        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(false);
    }

    @Test
    void updatesStatusWhenFound() {
        FollowUpReminder reminder = FollowUpReminder.create(
                UUID.randomUUID(), null, null, LocalDate.of(2026, 8, 30), NOW,
                ReminderType.GENERAL, null, UUID.randomUUID(), NOW);
        when(repository.findById(REMINDER_ID)).thenReturn(Optional.of(reminder));
        when(repository.save(any(FollowUpReminder.class))).thenAnswer(inv -> inv.getArgument(0));

        FollowUpReminderResult result = service.updateStatus(REMINDER_ID, ReminderStatus.SENT);

        assertEquals(ReminderStatus.SENT, result.status());
    }

    @Test
    void throwsNotFoundWhenMissing() {
        when(repository.findById(REMINDER_ID)).thenReturn(Optional.empty());

        assertThrows(FollowUpReminderNotFoundException.class,
                () -> service.updateStatus(REMINDER_ID, ReminderStatus.SENT));
    }

    @Test
    void rejectsNonAuthorizedRole() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.updateStatus(REMINDER_ID, ReminderStatus.SENT));
    }
}
