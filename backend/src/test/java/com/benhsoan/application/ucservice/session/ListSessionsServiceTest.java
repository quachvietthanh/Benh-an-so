package com.benhsoan.application.ucservice.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.domain.session.SessionSettings;
import com.benhsoan.port.dto.result.session.SessionSummaryResult;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;

@ExtendWith(MockitoExtension.class)
class ListSessionsServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-24T08:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();

    @Mock
    private UserSessionRepository userSessionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private SessionConfigurationProvider sessionConfigurationProvider;

    private ListSessionsService service() {
        return new ListSessionsService(userSessionRepository, userRepository, roleRepository,
                sessionConfigurationProvider, new SessionResultMapper());
    }

    @Test
    void listsOpenSessionsEnrichedWithUserAndRole() {
        PageRequest pageable = PageRequest.of(0, 20);
        UserSession session = UserSession.restore(SESSION_ID, USER_ID, "hash", null,
                NOW.plus(Duration.ofDays(7)), NOW, NOW, null);
        when(userSessionRepository.findAllByRevokedAtIsNull(pageable))
                .thenReturn(new PageImpl<>(List.of(session), pageable, 1));
        when(sessionConfigurationProvider.currentSettings())
                .thenReturn(new SessionSettings(Duration.ofMinutes(30), Duration.ofMinutes(5)));
        when(userRepository.findAllById(List.of(USER_ID))).thenReturn(List.of(
                User.restore(USER_ID, "admin", "hash", "Admin", "a@b.c", null, ROLE_ID, true, null, NOW)));
        when(roleRepository.findById(ROLE_ID)).thenReturn(java.util.Optional.of(
                Role.restore(ROLE_ID, "ADMIN", null, true, NOW, NOW, Set.of())));

        Page<SessionSummaryResult> result = service().list(pageable);

        assertEquals(1, result.getTotalElements());
        SessionSummaryResult summary = result.getContent().get(0);
        assertEquals(SESSION_ID, summary.sessionId());
        assertEquals("admin", summary.username());
        assertEquals("ADMIN", summary.role());
        assertEquals(NOW.plus(Duration.ofMinutes(30)), summary.expiresAt());
    }
}