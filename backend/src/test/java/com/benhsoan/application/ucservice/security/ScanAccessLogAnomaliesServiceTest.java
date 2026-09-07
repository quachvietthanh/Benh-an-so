package com.benhsoan.application.ucservice.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.config.AnomalyDetectionProperties;
import com.benhsoan.domain.medicalrecord.MedicalRecordAccessLog;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.security.SecurityAlert;
import com.benhsoan.domain.security.enums.AlertSeverity;
import com.benhsoan.domain.security.enums.AlertType;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordAccessLogRepository;
import com.benhsoan.port.outbound.repository.security.SecurityAlertRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class ScanAccessLogAnomaliesServiceTest {

    @Mock
    private MedicalRecordAccessLogRepository accessLogRepository;

    @Mock
    private SecurityAlertRepository securityAlertRepository;

    @Mock
    private ClockPort clockPort;

    private ScanAccessLogAnomaliesService service;

    @BeforeEach
    void setUp() {
        AnomalyDetectionProperties properties = new AnomalyDetectionProperties(
                20,
                LocalTime.of(7, 0),
                LocalTime.of(18, 0),
                ZoneId.of("Asia/Ho_Chi_Minh")
        );
        service = new ScanAccessLogAnomaliesService(
                accessLogRepository,
                securityAlertRepository,
                properties,
                clockPort
        );
    }

    @Test
    void generatesHighSeverityAlertWhenViewsExceedHourlyThreshold() {
        UUID user = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-11T10:05:00Z"); // 17:05 local, within working hours
        when(clockPort.now()).thenReturn(now);

        List<MedicalRecordAccessLog> views = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            views.add(view(user, now.plusSeconds(i)));
        }
        when(accessLogRepository.findViewsBetween(
                Instant.parse("2026-08-11T10:00:00Z"),
                Instant.parse("2026-08-11T11:00:00Z")
        )).thenReturn(views);

        service.scan();

        ArgumentCaptor<SecurityAlert> captor = ArgumentCaptor.forClass(SecurityAlert.class);
        verify(securityAlertRepository, times(1)).save(captor.capture());

        SecurityAlert alert = captor.getValue();
        assertEquals(AlertSeverity.HIGH, alert.getSeverity());
        assertEquals(AlertType.THRESHOLD_EXCEEDED, alert.getAlertType());
        assertEquals(21, alert.getAccessCount());
        assertEquals(user, alert.getUserId());
    }

    @Test
    void generatesLowSeverityAlertForOffHoursAccess() {
        UUID user = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-11T15:05:00Z"); // 22:05 local
        when(clockPort.now()).thenReturn(now);

        MedicalRecordAccessLog offHoursView = view(user, Instant.parse("2026-08-11T15:00:00Z")); // 22:00 local
        when(accessLogRepository.findViewsBetween(
                Instant.parse("2026-08-11T15:00:00Z"),
                Instant.parse("2026-08-11T16:00:00Z")
        )).thenReturn(List.of(offHoursView));

        service.scan();

        ArgumentCaptor<SecurityAlert> captor = ArgumentCaptor.forClass(SecurityAlert.class);
        verify(securityAlertRepository, times(1)).save(captor.capture());

        SecurityAlert alert = captor.getValue();
        assertEquals(AlertSeverity.LOW, alert.getSeverity());
        assertEquals(AlertType.OFF_HOURS_ACCESS, alert.getAlertType());
        assertEquals(1, alert.getAccessCount());
        assertEquals(user, alert.getUserId());
    }

    @Test
    void doesNotCreateAlertWhenActivityIsNormal() {
        UUID user = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-11T10:05:00Z"); // 17:05 local, within working hours
        when(clockPort.now()).thenReturn(now);

        List<MedicalRecordAccessLog> views = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            views.add(view(user, now.plusSeconds(i)));
        }
        when(accessLogRepository.findViewsBetween(any(), any())).thenReturn(views);

        service.scan();

        verify(securityAlertRepository, never()).save(any());
        verify(securityAlertRepository, never())
                .existsByUserIdAndAlertTypeAndWindowStart(any(), any(), any());
    }

    @Test
    void doesNothingWhenNoViewsInWindow() {
        when(clockPort.now()).thenReturn(Instant.parse("2026-08-11T10:05:00Z"));
        when(accessLogRepository.findViewsBetween(any(), any())).thenReturn(List.of());

        service.scan();

        verifyNoInteractions(securityAlertRepository);
    }

    @Test
    void skipsDuplicateAlertInSameDetectionWindow() {
        UUID user = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-11T10:05:00Z");
        when(clockPort.now()).thenReturn(now);

        List<MedicalRecordAccessLog> views = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            views.add(view(user, now.plusSeconds(i)));
        }
        when(accessLogRepository.findViewsBetween(any(), any())).thenReturn(views);
        when(securityAlertRepository.existsByUserIdAndAlertTypeAndWindowStart(any(), any(), any()))
                .thenReturn(true);

        service.scan();

        verify(securityAlertRepository, never()).save(any());
        verify(securityAlertRepository, atLeastOnce())
                .existsByUserIdAndAlertTypeAndWindowStart(any(), any(), any());
    }

    private MedicalRecordAccessLog view(UUID userId, Instant accessedAt) {
        return MedicalRecordAccessLog.createRecordAccess(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                userId,
                MedicalRecordAccessAction.VIEW,
                "Medical record viewed",
                accessedAt
        );
    }
}
