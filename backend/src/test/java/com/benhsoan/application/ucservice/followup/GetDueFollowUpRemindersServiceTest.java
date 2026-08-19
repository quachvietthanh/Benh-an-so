package com.benhsoan.application.ucservice.followup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.followup.FollowUpReminder;
import com.benhsoan.domain.followup.enums.ReminderStatus;
import com.benhsoan.domain.followup.enums.ReminderType;
import com.benhsoan.port.dto.result.FollowUpReminderResult;
import com.benhsoan.port.outbound.repository.followup.FollowUpReminderRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class GetDueFollowUpRemindersServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-15T08:00:00Z");

    private final FollowUpReminderRepository repository = mock(FollowUpReminderRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);
    private final FollowUpReminderAuthorizer authorizer = new FollowUpReminderAuthorizer(currentUserPort);
    private final FollowUpReminderResultMapper resultMapper = new FollowUpReminderResultMapper();

    private GetDueFollowUpRemindersService service;

    @BeforeEach
    void setUp() {
        service = new GetDueFollowUpRemindersService(repository, resultMapper, authorizer, clockPort);

        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(false);
        when(clockPort.now()).thenReturn(NOW);
    }

    @Test
    void queriesDueRemindersUsingCurrentInstant() {
        FollowUpReminder reminder = FollowUpReminder.create(
                UUID.randomUUID(), null, null, LocalDate.of(2026, 8, 14), NOW.minusSeconds(60),
                ReminderType.GENERAL, null, UUID.randomUUID(), NOW.minusSeconds(60));
        Page<FollowUpReminder> page = new PageImpl<>(List.of(reminder));

        when(repository.findDue(eq(NOW), any(), any(), any())).thenReturn(page);

        Page<FollowUpReminderResult> result = service.getDue(null, null, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals(ReminderStatus.PENDING, result.getContent().get(0).status());
        verify(repository).findDue(eq(NOW), eq(null), eq(null), any());
    }

    @Test
    void rejectsNonAuthorizedRole() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.getDue(null, null, PageRequest.of(0, 20)));

        verify(repository, never()).findDue(any(), any(), any(), any());
    }
}
