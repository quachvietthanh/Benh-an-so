package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.application.ucservice.patient.PatientAccessGuard;
import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.exception.AppointmentPastCutoffException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.dto.command.appointment.PatientCancelAppointmentCommand;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PatientCancelAppointmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-26T02:00:00Z");
    private static final Instant FUTURE_START = Instant.parse("2099-08-10T09:00:00Z");
    private static final Instant FUTURE_END = Instant.parse("2099-08-10T09:30:00Z");

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private PatientAccessGuard patientAccessGuard;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ClockPort clockPort;

    private PatientCancelAppointmentService service;

    @BeforeEach
    void setUp() {
        service = new PatientCancelAppointmentService(
                appointmentRepository,
                currentUserPort,
                patientAccessGuard,
                auditLogRepository,
                clockPort,
                new PatientAppointmentResultMapper(),
                new ObjectMapper()
        );
    }

    @Test
    void cancelsFutureScheduledAppointmentAndWritesAudit() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        Appointment appointment = Appointment.restore(appointmentId, "AP000300", patientId, doctorId,
                FUTURE_START, FUTURE_END, AppointmentStatus.SCHEDULED, "Khám tổng quát",
                null, null, null, actorId, Instant.parse("2026-08-01T00:00:00Z"));

        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
        when(patientAccessGuard.requirePatientOwnership(patientId)).thenReturn(mock(Patient.class));
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        when(appointmentRepository.save(appointment)).thenReturn(appointment);

        service.cancel(appointmentId, new PatientCancelAppointmentCommand("Bận việc đột xuất"));

        assertEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
        assertEquals("Bận việc đột xuất", appointment.getCancelReason());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog audit = captor.getValue();
        assertEquals(ActionType.CANCEL, audit.getActionType());
        assertEquals(ResourceType.APPOINTMENT, audit.getResourceType());
        assertEquals(appointmentId, audit.getResourceId());
        assertEquals(actorId, audit.getUserId());
    }

    @Test
    void rejectsPastAppointmentWithPastCutoff() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        Instant pastStart = Instant.parse("2026-08-25T09:00:00Z");
        Appointment appointment = Appointment.restore(appointmentId, "AP000301", patientId, doctorId,
                pastStart, pastStart.plusSeconds(1800), AppointmentStatus.SCHEDULED, "Khám tổng quát",
                null, null, null, actorId, Instant.parse("2026-08-01T00:00:00Z"));

        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
        when(patientAccessGuard.requirePatientOwnership(patientId)).thenReturn(mock(Patient.class));
        when(clockPort.now()).thenReturn(NOW);

        assertThrows(AppointmentPastCutoffException.class,
                () -> service.cancel(appointmentId, new PatientCancelAppointmentCommand("Bận việc")));
    }

    @Test
    void rejectsCrossPatientAccessWithForbidden() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        Appointment appointment = Appointment.restore(appointmentId, "AP000302", patientId, doctorId,
                FUTURE_START, FUTURE_END, AppointmentStatus.SCHEDULED, "Khám tổng quát",
                null, null, null, actorId, Instant.parse("2026-08-01T00:00:00Z"));

        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
        when(patientAccessGuard.requirePatientOwnership(patientId))
                .thenThrow(new AccessDeniedException("Patient may only access their own data."));

        assertThrows(AccessDeniedException.class,
                () -> service.cancel(appointmentId, new PatientCancelAppointmentCommand("Bận việc")));
        verify(patientAccessGuard).requirePatientOwnership(patientId);
    }
}
