package com.benhsoan.application.ucservice.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.domain.auth.exception.SessionExpiredException;
import com.benhsoan.domain.auth.exception.SessionNotFoundException;
import com.benhsoan.port.dto.command.session.TerminateSessionCommand;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class TerminateSessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-24T08:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID TARGET_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();

    @Mock
    private UserSessionRepository userSessionRepository;
    @Mock
    private CurrentUserPort currentUserPort;
    @Mock
    private ClockPort clockPort;
    @Mock
    private AuditLogRepository auditLogRepository;

    private TerminateSessionService service() {
        return new TerminateSessionService(userSessionRepository, currentUserPort, clockPort, auditLogRepository);
    }

    private void configureActor() {
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
        when(clockPort.now()).thenReturn(NOW);
    }

    @Test
    void terminatesSessionAndAuditsWithReason() {
        configureActor();
        UserSession session = UserSession.restore(SESSION_ID, TARGET_ID, "hash", null,
                NOW.plus(Duration.ofDays(7)), NOW, NOW, null);
        when(userSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(userSessionRepository.revokeById(SESSION_ID, NOW)).thenReturn(true);

        service().terminate(new TerminateSessionCommand(SESSION_ID, "Unattended workstation"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog audit = captor.getValue();
        assertEquals(ActionType.SESSION_TERMINATE, audit.getActionType());
        assertEquals(ResourceType.USER_SESSION, audit.getResourceType());
        assertEquals(ACTOR_ID, audit.getUserId());
        assertEquals(SESSION_ID, audit.getResourceId());
        assertEquals("{\"reason\":\"Unattended workstation\"}", audit.getDetail());
    }

    @Test
    void rejectsUnknownSession() {
        configureActor();
        when(userSessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        assertThrows(SessionNotFoundException.class,
                () -> service().terminate(new TerminateSessionCommand(SESSION_ID, null)));

        verify(userSessionRepository, never()).revokeById(any(), any());
    }

    @Test
    void rejectsAlreadyRevokedSession() {
        configureActor();
        UserSession session = UserSession.restore(SESSION_ID, TARGET_ID, "hash", null,
                NOW.plus(Duration.ofDays(7)), NOW, NOW, NOW);
        when(userSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        assertThrows(SessionExpiredException.class,
                () -> service().terminate(new TerminateSessionCommand(SESSION_ID, null)));

        verify(userSessionRepository, never()).revokeById(any(), any());
    }

    @Test
    void rejectsConcurrentTermination() {
        configureActor();
        UserSession session = UserSession.restore(SESSION_ID, TARGET_ID, "hash", null,
                NOW.plus(Duration.ofDays(7)), NOW, NOW, null);
        when(userSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(userSessionRepository.revokeById(SESSION_ID, NOW)).thenReturn(false);

        assertThrows(SessionExpiredException.class,
                () -> service().terminate(new TerminateSessionCommand(SESSION_ID, null)));

        verify(auditLogRepository, never()).save(any());
    }
}