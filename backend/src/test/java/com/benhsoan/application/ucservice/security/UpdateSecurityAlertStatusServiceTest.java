package com.benhsoan.application.ucservice.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.security.SecurityAlert;
import com.benhsoan.domain.security.enums.AlertSeverity;
import com.benhsoan.domain.security.enums.AlertStatus;
import com.benhsoan.domain.security.enums.AlertType;
import com.benhsoan.domain.security.exception.SecurityAlertNotFoundException;
import com.benhsoan.port.dto.result.security.SecurityAlertResult;
import com.benhsoan.port.outbound.repository.security.SecurityAlertRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class UpdateSecurityAlertStatusServiceTest {

    @Mock
    private SecurityAlertRepository securityAlertRepository;

    private static final Instant NOW = Instant.parse("2026-08-11T10:05:00Z");

    private UpdateSecurityAlertStatusService service;

    @BeforeEach
    void setUp() {
        ClockPort clockPort = () -> NOW;
        service = new UpdateSecurityAlertStatusService(securityAlertRepository, clockPort);
    }

    @Test
    void updatesStatusAndSavesExistingAlert() {
        UUID alertId = UUID.randomUUID();
        SecurityAlert alert = SecurityAlert.create(
                UUID.randomUUID(),
                AlertType.THRESHOLD_EXCEEDED,
                AlertSeverity.HIGH,
                "Threshold exceeded",
                21,
                NOW.minus(1, ChronoUnit.HOURS),
                NOW,
                NOW);

        when(securityAlertRepository.findById(alertId)).thenReturn(Optional.of(alert));
        when(securityAlertRepository.save(any(SecurityAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SecurityAlertResult result = service.updateStatus(alertId, AlertStatus.READ);

        assertEquals(AlertStatus.READ, result.status());
        assertEquals(AlertStatus.READ, alert.getStatus());
        verify(securityAlertRepository).save(alert);
    }

    @Test
    void throwsNotFoundWhenAlertMissing() {
        UUID alertId = UUID.randomUUID();
        when(securityAlertRepository.findById(alertId)).thenReturn(Optional.empty());

        assertThrows(SecurityAlertNotFoundException.class,
                () -> service.updateStatus(alertId, AlertStatus.DISMISSED));
    }
}
