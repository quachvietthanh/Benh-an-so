package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.application.ucservice.patient.PatientAccessGuard;
import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.exception.AppointmentInvalidStatusException;
import com.benhsoan.domain.appointment.exception.AppointmentNotFoundException;
import com.benhsoan.domain.appointment.exception.AppointmentPastCutoffException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.dto.result.appointment.PatientAppointmentResult;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

class PatientConfirmAppointmentServiceTest {

    private AppointmentRepository appointmentRepository;
    private CurrentUserPort currentUserPort;
    private PatientAccessGuard patientAccessGuard;
    private AuditLogRepository auditLogRepository;
    private ClockPort clockPort;
    private PatientAppointmentResultMapper resultMapper;
    private ObjectMapper objectMapper;

    private PatientConfirmAppointmentService service;

    private final Instant now = Instant.parse("2026-09-14T08:00:00Z");
    private final UUID currentUserId = UUID.randomUUID();
    private final UUID appointmentId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        currentUserPort = mock(CurrentUserPort.class);
        patientAccessGuard = mock(PatientAccessGuard.class);
        auditLogRepository = mock(AuditLogRepository.class);
        clockPort = mock(ClockPort.class);
        resultMapper = new PatientAppointmentResultMapper();
        objectMapper = new ObjectMapper();

        when(clockPort.now()).thenReturn(now);
        when(currentUserPort.getCurrentUserId()).thenReturn(currentUserId);

        service = new PatientConfirmAppointmentService(
                appointmentRepository,
                currentUserPort,
                patientAccessGuard,
                auditLogRepository,
                clockPort,
                resultMapper,
                objectMapper
        );
    }

    @Test
    void patientConfirmsAppointmentSuccessfully_TC02() {
        Appointment appointment = Appointment.restore(
                appointmentId, "APT-P01", patientId, doctorId,
                now.plusSeconds(3600), now.plusSeconds(5400),
                AppointmentStatus.SCHEDULED, "Tái khám tim mạch",
                null, null, null, currentUserId, now.minusSeconds(7200),
                "ONLINE_PORTAL"
        );

        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientAppointmentResult result = service.confirm(appointmentId);

        assertNotNull(result);
        assertEquals(AppointmentStatus.CONFIRMED, result.status());
        assertEquals(now, result.confirmedAt());

        verify(patientAccessGuard).requirePatientOwnership(eq(patientId), eq(ResourceType.APPOINTMENT), eq(appointmentId));
        verify(appointmentRepository).save(appointment);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void rejectsCrossPatientConfirmation_QTN23() {
        Appointment appointment = Appointment.restore(
                appointmentId, "APT-P02", patientId, doctorId,
                now.plusSeconds(3600), now.plusSeconds(5400),
                AppointmentStatus.SCHEDULED, "Tái khám",
                null, null, null, UUID.randomUUID(), now.minusSeconds(7200)
        );

        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
        doThrow(new AccessDeniedException("Patient may only access their own data."))
                .when(patientAccessGuard).requirePatientOwnership(eq(patientId), eq(ResourceType.APPOINTMENT), eq(appointmentId));

        assertThrows(AccessDeniedException.class, () -> service.confirm(appointmentId));
        assertEquals(AppointmentStatus.SCHEDULED, appointment.getStatus());
        verify(appointmentRepository, never()).save(appointment);
    }

    @Test
    void rejectsWhenAppointmentAlreadyCancelled_TC03() {
        Appointment appointment = Appointment.restore(
                appointmentId, "APT-P03", patientId, doctorId,
                now.plusSeconds(3600), now.plusSeconds(5400),
                AppointmentStatus.CANCELLED, "Tái khám",
                "Hủy lịch", null, null, currentUserId, now.minusSeconds(7200)
        );

        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));

        assertThrows(AppointmentInvalidStatusException.class, () -> service.confirm(appointmentId));
        verify(appointmentRepository, never()).save(appointment);
    }

    @Test
    void rejectsWhenAppointmentPastCutoff() {
        Appointment appointment = Appointment.restore(
                appointmentId, "APT-P04", patientId, doctorId,
                now.minusSeconds(300), now.plusSeconds(1500),
                AppointmentStatus.SCHEDULED, "Tái khám",
                null, null, null, currentUserId, now.minusSeconds(7200)
        );

        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));

        assertThrows(AppointmentPastCutoffException.class, () -> service.confirm(appointmentId));
        verify(appointmentRepository, never()).save(appointment);
    }

    @Test
    void throwsNotFoundWhenAppointmentDoesNotExist() {
        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.empty());

        assertThrows(AppointmentNotFoundException.class, () -> service.confirm(appointmentId));
    }

    @Test
    void rejectsConfirmationWhenAppointmentAlreadyConfirmed_DoubleConfirmation() {
        Appointment appointment = Appointment.restore(
                appointmentId, "APT-P05", patientId, doctorId,
                now.plusSeconds(3600), now.plusSeconds(5400),
                AppointmentStatus.CONFIRMED, "Tái khám",
                null, null, null, currentUserId, now.minusSeconds(7200),
                "ONLINE_PORTAL", now.minusSeconds(300), currentUserId
        );

        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));

        assertThrows(AppointmentInvalidStatusException.class, () -> service.confirm(appointmentId));
        assertEquals(AppointmentStatus.CONFIRMED, appointment.getStatus());
        verify(appointmentRepository, never()).save(appointment);
    }
}
