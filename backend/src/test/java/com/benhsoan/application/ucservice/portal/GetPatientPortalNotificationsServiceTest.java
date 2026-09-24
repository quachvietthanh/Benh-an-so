package com.benhsoan.application.ucservice.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.portal.notification.PatientPortalNotification;
import com.benhsoan.domain.portal.notification.PatientPortalNotificationType;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.portal.PatientPortalNotificationRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

@ExtendWith(MockitoExtension.class)
class GetPatientPortalNotificationsServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-30T01:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();

    @Mock private PatientPortalNotificationRepository notificationRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private CurrentUserPort currentUserPort;

    private GetPatientPortalNotificationsService service;

    @BeforeEach
    void setUp() {
        service = new GetPatientPortalNotificationsService(
                notificationRepository, patientRepository, currentUserPort,
                new PatientPortalNotificationResultMapper());
    }

    @Test
    void returnsOwnNotifications() {
        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(PATIENT_ID);
        when(currentUserPort.getCurrentUserId()).thenReturn(USER_ID);
        when(patientRepository.findByUserId(USER_ID)).thenReturn(java.util.Optional.of(patient));
        when(notificationRepository.findByPatientIdOrderByCreatedAtDesc(PATIENT_ID, 50))
                .thenReturn(List.of(
                        PatientPortalNotification.reminder(PATIENT_ID, "t", "m", UUID.randomUUID(), NOW)));

        var result = service.getNotifications(50);

        assertEquals(1, result.size());
        assertEquals(PatientPortalNotificationType.APPOINTMENT_REMINDER, result.get(0).type());
    }

    @Test
    void rejectsWhenNoPatientProfileIsLinked() {
        when(currentUserPort.getCurrentUserId()).thenReturn(USER_ID);
        when(patientRepository.findByUserId(USER_ID)).thenReturn(java.util.Optional.empty());

        assertThrows(AccessDeniedException.class, () -> service.getNotifications(50));
    }
}
