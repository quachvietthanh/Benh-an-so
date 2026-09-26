package com.benhsoan.application.ucservice.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.security.SecurityAlert;
import com.benhsoan.domain.security.enums.AlertSeverity;
import com.benhsoan.domain.security.enums.AlertType;
import com.benhsoan.port.dto.result.security.SecurityAlertResult;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.security.SecurityAlertRepository;

@ExtendWith(MockitoExtension.class)
class GetSecurityAlertsServiceTest {

    @Mock
    private SecurityAlertRepository securityAlertRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void enrichesAlertsWithUsernameAndFullName() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-11T10:05:00Z");
        SecurityAlert alert = SecurityAlert.create(userId, AlertType.THRESHOLD_EXCEEDED, AlertSeverity.HIGH,
                "desc", 21, now.minus(1, ChronoUnit.HOURS), now, now);

        when(securityAlertRepository.findAll(any()))
                .thenReturn(new PageImpl<>(List.of(alert), PageRequest.of(0, 20), 1));

        User user = User.restore(userId, "admin", "hash", "System Administrator", "admin@example.com",
                null, UUID.randomUUID(), true, null, now);
        when(userRepository.findAllById(any())).thenReturn(List.of(user));

        GetSecurityAlertsService service = new GetSecurityAlertsService(securityAlertRepository, userRepository);
        var result = service.getSecurityAlerts(PageRequest.of(0, 20));

        SecurityAlertResult item = result.getContent().get(0);
        assertEquals("admin", item.username());
        assertEquals("System Administrator", item.fullName());
        assertEquals(21, item.accessCount());
    }

    @Test
    void mapsMissingUserToNullNames() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-11T10:05:00Z");
        SecurityAlert alert = SecurityAlert.create(userId, AlertType.OFF_HOURS_ACCESS, AlertSeverity.LOW,
                "desc", 1, now.minus(1, ChronoUnit.HOURS), now, now);

        when(securityAlertRepository.findAll(any()))
                .thenReturn(new PageImpl<>(List.of(alert), PageRequest.of(0, 20), 1));
        when(userRepository.findAllById(any())).thenReturn(List.of());

        GetSecurityAlertsService service = new GetSecurityAlertsService(securityAlertRepository, userRepository);
        var result = service.getSecurityAlerts(PageRequest.of(0, 20));

        SecurityAlertResult item = result.getContent().get(0);
        assertNull(item.username());
        assertNull(item.fullName());
    }
}
