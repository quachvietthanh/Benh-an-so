package com.benhsoan.application.ucservice.portal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.AppointmentRescheduleLog;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.portal.notification.AppointmentChangedNotificationRequested;
import com.benhsoan.domain.portal.notification.AppointmentReminderNotificationRequested;
import com.benhsoan.domain.portal.notification.LabResultAvailableNotificationRequested;

@ExtendWith(MockitoExtension.class)
class PatientPortalNotificationEventListenerTest {

    private static final Instant NOW = Instant.parse("2026-09-30T01:00:00Z");
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID CLINICAL_RESULT_ID = UUID.randomUUID();

    @Mock private PatientPortalNotificationCreator creator;

    private PatientPortalNotificationEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new PatientPortalNotificationEventListener(creator);
    }

    @Test
    void reminderEventDelegatesToCreator() {
        Appointment appointment = mock(Appointment.class);
        Patient patient = mock(Patient.class);
        User doctor = mock(User.class);

        listener.onAppointmentReminderRequested(
                new AppointmentReminderNotificationRequested(appointment, patient, doctor, NOW));

        verify(creator).createAppointmentReminder(appointment, patient, doctor, NOW);
    }

    @Test
    void changedEventDelegatesToCreator() {
        Appointment appointment = mock(Appointment.class);
        AppointmentRescheduleLog log = mock(AppointmentRescheduleLog.class);

        listener.onAppointmentChangedRequested(
                new AppointmentChangedNotificationRequested(appointment, log, NOW));

        verify(creator).createAppointmentChanged(appointment, log, NOW);
    }

    @Test
    void labResultEventDelegatesToCreator() {
        listener.onLabResultAvailableRequested(
                new LabResultAvailableNotificationRequested(PATIENT_ID, CLINICAL_RESULT_ID, NOW));

        verify(creator).createLabResultAvailable(PATIENT_ID, CLINICAL_RESULT_ID, NOW);
    }

    @Test
    void reminderEventSwallowsPersistenceFailure() {
        doThrow(new DataIntegrityViolationException("duplicate notification"))
                .when(creator).createAppointmentReminder(any(), any(), any(), any());

        AppointmentReminderNotificationRequested event = new AppointmentReminderNotificationRequested(
                mock(Appointment.class), mock(Patient.class), mock(User.class), NOW);

        assertDoesNotThrow(() -> listener.onAppointmentReminderRequested(event));
    }

    @Test
    void reminderEventPropagatesNonPersistenceFailure() {
        doThrow(new IllegalStateException("programming bug"))
                .when(creator).createAppointmentReminder(any(), any(), any(), any());

        AppointmentReminderNotificationRequested event = new AppointmentReminderNotificationRequested(
                mock(Appointment.class), mock(Patient.class), mock(User.class), NOW);

        assertThrows(IllegalStateException.class,
                () -> listener.onAppointmentReminderRequested(event));
    }
}
