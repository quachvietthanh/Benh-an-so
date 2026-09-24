package com.benhsoan.application.ucservice.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.AppointmentRescheduleLog;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.portal.notification.PatientPortalNotification;
import com.benhsoan.domain.portal.notification.PatientPortalNotificationType;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.portal.PatientPortalNotificationRepository;

@ExtendWith(MockitoExtension.class)
class PatientPortalNotificationCreatorTest {

    private static final Instant NOW = Instant.parse("2026-09-30T01:00:00Z");
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID PORTAL_USER_ID = UUID.randomUUID();
    private static final UUID APPOINTMENT_ID = UUID.randomUUID();
    private static final UUID RESCHEDULE_LOG_ID = UUID.randomUUID();
    private static final UUID CLINICAL_RESULT_ID = UUID.randomUUID();

    @Mock private PatientPortalNotificationRepository notificationRepository;
    @Mock private PatientRepository patientRepository;

    private PatientPortalNotificationCreator creator;

    @BeforeEach
    void setUp() {
        creator = new PatientPortalNotificationCreator(notificationRepository, patientRepository);
    }

    @Test
    void createsReminderForPortalLinkedPatientAndCorrectType() {
        Appointment appointment = appointment();
        Patient patient = patient(PORTAL_USER_ID);
        User doctor = mock(User.class);
        when(doctor.getFullName()).thenReturn("Bác sĩ An");
        when(notificationRepository.existsByPatientIdAndTypeAndAppointmentId(
                PATIENT_ID, PatientPortalNotificationType.APPOINTMENT_REMINDER, APPOINTMENT_ID))
                .thenReturn(false);

        creator.createAppointmentReminder(appointment, patient, doctor, NOW);

        ArgumentCaptor<PatientPortalNotification> captor =
                ArgumentCaptor.forClass(PatientPortalNotification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(PatientPortalNotificationType.APPOINTMENT_REMINDER, captor.getValue().getType());
        assertEquals(PATIENT_ID, captor.getValue().getPatientId());
    }

    @Test
    void skipsReminderWhenPatientHasNoPortalAccount() {
        creator.createAppointmentReminder(appointment(), patient(null), mock(User.class), NOW);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void skipsReminderWhenAlreadyExists() {
        when(notificationRepository.existsByPatientIdAndTypeAndAppointmentId(
                PATIENT_ID, PatientPortalNotificationType.APPOINTMENT_REMINDER, APPOINTMENT_ID))
                .thenReturn(true);

        creator.createAppointmentReminder(appointment(), patient(PORTAL_USER_ID), mock(User.class), NOW);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createsChangedNotificationForCorrectPatient() {
        Appointment appointment = appointment();
        AppointmentRescheduleLog log = rescheduleLog();
        Patient patient = patient(PORTAL_USER_ID);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(notificationRepository.existsByPatientIdAndTypeAndRescheduleLogId(
                PATIENT_ID, PatientPortalNotificationType.APPOINTMENT_CHANGED, RESCHEDULE_LOG_ID))
                .thenReturn(false);

        creator.createAppointmentChanged(appointment, log, NOW);

        ArgumentCaptor<PatientPortalNotification> captor =
                ArgumentCaptor.forClass(PatientPortalNotification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(PatientPortalNotificationType.APPOINTMENT_CHANGED, captor.getValue().getType());
        assertEquals(PATIENT_ID, captor.getValue().getPatientId());
        assertEquals(RESCHEDULE_LOG_ID, captor.getValue().getRescheduleLogId());
    }

    @Test
    void skipsChangedNotificationWhenAlreadyExists() {
        Patient patient = patient(PORTAL_USER_ID);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(notificationRepository.existsByPatientIdAndTypeAndRescheduleLogId(
                PATIENT_ID, PatientPortalNotificationType.APPOINTMENT_CHANGED, RESCHEDULE_LOG_ID))
                .thenReturn(true);

        creator.createAppointmentChanged(appointment(), rescheduleLog(), NOW);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void skipsChangedNotificationWhenPatientHasNoPortalAccount() {
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(mock(Patient.class)));

        creator.createAppointmentChanged(appointment(), rescheduleLog(), NOW);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createsLabResultNotificationForCorrectPatient() {
        Patient patient = patient(PORTAL_USER_ID);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(notificationRepository.existsByPatientIdAndTypeAndClinicalResultId(
                PATIENT_ID, PatientPortalNotificationType.LAB_RESULT_AVAILABLE, CLINICAL_RESULT_ID))
                .thenReturn(false);

        creator.createLabResultAvailable(PATIENT_ID, CLINICAL_RESULT_ID, NOW);

        ArgumentCaptor<PatientPortalNotification> captor =
                ArgumentCaptor.forClass(PatientPortalNotification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(PatientPortalNotificationType.LAB_RESULT_AVAILABLE, captor.getValue().getType());
        assertEquals(PATIENT_ID, captor.getValue().getPatientId());
        assertEquals(CLINICAL_RESULT_ID, captor.getValue().getClinicalResultId());
    }

    @Test
    void skipsLabResultNotificationWhenAlreadyExists() {
        Patient patient = patient(PORTAL_USER_ID);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(notificationRepository.existsByPatientIdAndTypeAndClinicalResultId(
                PATIENT_ID, PatientPortalNotificationType.LAB_RESULT_AVAILABLE, CLINICAL_RESULT_ID))
                .thenReturn(true);

        creator.createLabResultAvailable(PATIENT_ID, CLINICAL_RESULT_ID, NOW);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void skipsLabResultNotificationWhenPatientHasNoPortalAccount() {
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(mock(Patient.class)));

        creator.createLabResultAvailable(PATIENT_ID, CLINICAL_RESULT_ID, NOW);

        verify(notificationRepository, never()).save(any());
    }

    private Appointment appointment() {
        return Appointment.restore(APPOINTMENT_ID, "APT000123", PATIENT_ID, UUID.randomUUID(),
                Instant.parse("2026-10-01T02:00:00Z"), Instant.parse("2026-10-01T02:30:00Z"),
                AppointmentStatus.SCHEDULED, "Khám tổng quát", null, null, null,
                UUID.randomUUID(), NOW);
    }

    private AppointmentRescheduleLog rescheduleLog() {
        return AppointmentRescheduleLog.restore(RESCHEDULE_LOG_ID, APPOINTMENT_ID,
                UUID.randomUUID(), UUID.randomUUID(),
                Instant.parse("2026-10-01T02:00:00Z"), Instant.parse("2026-10-01T02:30:00Z"),
                Instant.parse("2026-10-02T03:00:00Z"), Instant.parse("2026-10-02T03:30:00Z"),
                "Dời lịch", UUID.randomUUID(), NOW);
    }

    private Patient patient(UUID userId) {
        Patient patient = mock(Patient.class);
        when(patient.getUserId()).thenReturn(userId);
        return patient;
    }
}
