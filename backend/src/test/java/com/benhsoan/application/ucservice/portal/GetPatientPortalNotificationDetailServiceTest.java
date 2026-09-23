package com.benhsoan.application.ucservice.portal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.application.ucservice.patient.PatientAccessGuard;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.portal.exception.PatientPortalNotificationNotFoundException;
import com.benhsoan.domain.portal.notification.PatientPortalNotification;
import com.benhsoan.domain.portal.notification.PatientPortalNotificationType;
import com.benhsoan.port.outbound.repository.portal.PatientPortalNotificationRepository;

@ExtendWith(MockitoExtension.class)
class GetPatientPortalNotificationDetailServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-30T01:00:00Z");
    private static final UUID NOTIFICATION_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();

    @Mock private PatientPortalNotificationRepository notificationRepository;
    @Mock private PatientAccessGuard patientAccessGuard;

    private GetPatientPortalNotificationDetailService service;

    @BeforeEach
    void setUp() {
        service = new GetPatientPortalNotificationDetailService(
                notificationRepository, patientAccessGuard, new PatientPortalNotificationResultMapper());
    }

    @Test
    void returnsOwnNotificationDetail() {
        PatientPortalNotification notification = notification();
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

        service.getNotification(NOTIFICATION_ID);

        verify(patientAccessGuard).requirePatientOwnership(
                PATIENT_ID, ResourceType.PATIENT_PORTAL, NOTIFICATION_ID);
    }

    @Test
    void missingNotificationThrowsNotFound() {
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

        assertThrows(PatientPortalNotificationNotFoundException.class,
                () -> service.getNotification(NOTIFICATION_ID));
    }

    @Test
    void foreignNotificationIsRejected() {
        when(notificationRepository.findById(NOTIFICATION_ID))
                .thenReturn(Optional.of(notification()));
        doThrow(new AccessDeniedException("denied")).when(patientAccessGuard)
                .requirePatientOwnership(any(), any(), any());

        assertThrows(AccessDeniedException.class, () -> service.getNotification(NOTIFICATION_ID));
    }

    private PatientPortalNotification notification() {
        return PatientPortalNotification.restore(
                NOTIFICATION_ID, PATIENT_ID, PatientPortalNotificationType.APPOINTMENT_REMINDER,
                "t", "m", null, NOW, UUID.randomUUID(), null, null);
    }
}
