package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.config.AppointmentReminderProperties;
import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.notification.AppointmentNotificationLog;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.port.outbound.notification.AppointmentNotificationPort;
import com.benhsoan.port.outbound.notification.AppointmentReminderMessage;
import com.benhsoan.port.outbound.notification.NotificationSendResult;
import com.benhsoan.port.outbound.repository.crudRepository.appointment.AppointmentNotificationLogRepository;
import com.benhsoan.port.outbound.repository.crudRepository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.crudRepository.patient.PatientRepository;

@ExtendWith(MockitoExtension.class)
class ProcessAppointmentReminderServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-19T02:00:00Z");
    private static final UUID APPOINTMENT_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private AppointmentNotificationLogRepository notificationLogRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private UserRepository userRepository;
    @Mock private AppointmentNotificationPort appointmentNotificationPort;
    @Captor private ArgumentCaptor<AppointmentNotificationLog> logCaptor;
    @Captor private ArgumentCaptor<AppointmentReminderMessage> messageCaptor;

    private ProcessAppointmentReminderService service;

    @BeforeEach
    void setUp() {
        service = new ProcessAppointmentReminderService(appointmentRepository,
                notificationLogRepository, patientRepository, userRepository,
                appointmentNotificationPort,
                new AppointmentReminderProperties(true, 24, 60_000, ZoneId.of("Asia/Ho_Chi_Minh")));
    }

    @Test
    void sendsScheduledAppointmentAndWritesSentLog() {
        Appointment appointment = appointment(AppointmentStatus.SCHEDULED);
        prepareEligibleAppointment(appointment);
        when(appointmentNotificationPort.sendAppointmentReminder(any()))
                .thenReturn(NotificationSendResult.delivered());

        service.process(APPOINTMENT_ID, NOW);

        verify(appointmentNotificationPort).sendAppointmentReminder(messageCaptor.capture());
        verify(notificationLogRepository).save(logCaptor.capture());
        assertEquals("AP000123", messageCaptor.getValue().appointmentCode());
        assertEquals("Nguyen Van A", messageCaptor.getValue().patientName());
        assertEquals("Nguyen Van B", messageCaptor.getValue().doctorName());
        assertEquals("SENT", logCaptor.getValue().getStatus().name());
    }

    @Test
    void sendsConfirmedAppointment() {
        Appointment appointment = appointment(AppointmentStatus.CONFIRMED);
        prepareEligibleAppointment(appointment);
        when(appointmentNotificationPort.sendAppointmentReminder(any()))
                .thenReturn(NotificationSendResult.delivered());

        service.process(APPOINTMENT_ID, NOW);

        verify(appointmentNotificationPort).sendAppointmentReminder(any());
        verify(notificationLogRepository).save(any());
    }

    @Test
    void doesNotSendCancelledAppointment() {
        when(appointmentRepository.findByIdForUpdate(APPOINTMENT_ID))
                .thenReturn(Optional.of(appointment(AppointmentStatus.CANCELLED)));

        service.process(APPOINTMENT_ID, NOW);

        verify(appointmentNotificationPort, never()).sendAppointmentReminder(any());
        verify(notificationLogRepository, never()).save(any());
    }

    @Test
    void doesNotSendCompletedNoShowOrInProgressAppointments() {
        for (AppointmentStatus status : new AppointmentStatus[]{
                AppointmentStatus.COMPLETED, AppointmentStatus.NO_SHOW,
                AppointmentStatus.IN_PROGRESS}) {
            when(appointmentRepository.findByIdForUpdate(APPOINTMENT_ID))
                    .thenReturn(Optional.of(appointment(status)));

            service.process(APPOINTMENT_ID, NOW);
        }

        verify(appointmentNotificationPort, never()).sendAppointmentReminder(any());
        verify(notificationLogRepository, never()).save(any());
    }

    @Test
    void doesNotSendAppointmentThatHasAlreadyStarted() {
        Appointment appointment = Appointment.restore(APPOINTMENT_ID, "AP000123", PATIENT_ID,
                DOCTOR_ID, NOW, NOW.plusSeconds(1_800), AppointmentStatus.SCHEDULED,
                "Kham tong quat", null, null, null, UUID.randomUUID(), NOW.minusSeconds(86_400));
        when(appointmentRepository.findByIdForUpdate(APPOINTMENT_ID))
                .thenReturn(Optional.of(appointment));

        service.process(APPOINTMENT_ID, NOW);

        verify(appointmentNotificationPort, never()).sendAppointmentReminder(any());
    }

    @Test
    void doesNotSendWhenSentLogAlreadyExists() {
        when(appointmentRepository.findByIdForUpdate(APPOINTMENT_ID))
                .thenReturn(Optional.of(appointment(AppointmentStatus.SCHEDULED)));
        when(notificationLogRepository.existsSentReminderByAppointmentId(APPOINTMENT_ID))
                .thenReturn(true);

        service.process(APPOINTMENT_ID, NOW);

        verify(appointmentNotificationPort, never()).sendAppointmentReminder(any());
        verify(notificationLogRepository, never()).save(any());
    }

    @Test
    void writesFailedLogWhenAdapterReportsFailure() {
        prepareEligibleAppointment(appointment(AppointmentStatus.SCHEDULED));
        when(appointmentNotificationPort.sendAppointmentReminder(any()))
                .thenReturn(NotificationSendResult.failed("Mock delivery failure"));

        service.process(APPOINTMENT_ID, NOW);

        verify(notificationLogRepository).save(logCaptor.capture());
        assertEquals("FAILED", logCaptor.getValue().getStatus().name());
        assertEquals("Mock delivery failure", logCaptor.getValue().getFailureReason());
    }

    private void prepareEligibleAppointment(Appointment appointment) {
        when(appointmentRepository.findByIdForUpdate(APPOINTMENT_ID))
                .thenReturn(Optional.of(appointment));
        when(notificationLogRepository.existsSentReminderByAppointmentId(APPOINTMENT_ID))
                .thenReturn(false);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient()));
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor()));
    }

    private Appointment appointment(AppointmentStatus status) {
        return Appointment.restore(APPOINTMENT_ID, "AP000123", PATIENT_ID, DOCTOR_ID,
                NOW.plusSeconds(3_600), NOW.plusSeconds(5_400), status, "Kham tong quat",
                null, null, null, UUID.randomUUID(), NOW.minusSeconds(86_400));
    }

    private Patient patient() {
        return Patient.restore(PATIENT_ID, "BN000123", "Nguyen Van A",
                LocalDate.of(1990, 1, 1), Gender.MALE, null, null, null, "0123456789", null,
                null, null, null, true, NOW, NOW, null, UUID.randomUUID());
    }

    private User doctor() {
        return User.restore(DOCTOR_ID, "doctor", "hash", "Nguyen Van B", "doctor@test.com",
                null, UUID.randomUUID(), true, null, NOW);
    }
}
