package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.exception.AppointmentInvalidStatusException;
import com.benhsoan.domain.appointment.exception.AppointmentNotFoundException;
import com.benhsoan.domain.appointment.exception.AppointmentPastCutoffException;
import com.benhsoan.domain.appointment.exception.UnauthorizedAppointmentOperationException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auth.User;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

class ConfirmAppointmentServiceTest {

    private AppointmentRepository appointmentRepository;
    private UserRepository userRepository;
    private CurrentUserPort currentUserPort;
    private AuditLogRepository auditLogRepository;
    private AppointmentAccessDeniedAuditWriter accessDeniedAuditWriter;
    private ClockPort clockPort;
    private AppointmentResultMapper resultMapper;
    private ObjectMapper objectMapper;

    private ConfirmAppointmentService service;

    private final Instant now = Instant.parse("2026-09-14T08:00:00Z");
    private final UUID currentUserId = UUID.randomUUID();
    private final UUID appointmentId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        userRepository = mock(UserRepository.class);
        currentUserPort = mock(CurrentUserPort.class);
        auditLogRepository = mock(AuditLogRepository.class);
        accessDeniedAuditWriter = mock(AppointmentAccessDeniedAuditWriter.class);
        clockPort = mock(ClockPort.class);
        resultMapper = new AppointmentResultMapper();
        objectMapper = new ObjectMapper();

        when(clockPort.now()).thenReturn(now);
        when(currentUserPort.getCurrentUserId()).thenReturn(currentUserId);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);

        service = new ConfirmAppointmentService(
                appointmentRepository,
                userRepository,
                currentUserPort,
                auditLogRepository,
                accessDeniedAuditWriter,
                clockPort,
                resultMapper,
                objectMapper
        );
    }

    @Test
    void confirmsAppointmentSuccessfullyWhenScheduledAndFuture_TC01() {
        Appointment appointment = Appointment.restore(
                appointmentId, "APT-001", patientId, doctorId,
                now.plusSeconds(3600), now.plusSeconds(5400),
                AppointmentStatus.SCHEDULED, "Khám tổng quát",
                null, null, null, UUID.randomUUID(), now.minusSeconds(7200)
        );

        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = mock(User.class);
        when(user.getFullName()).thenReturn("Lễ Tân Nguyễn Văn A");
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(user));

        AppointmentResult result = service.confirm(appointmentId);

        assertNotNull(result);
        assertEquals(AppointmentStatus.CONFIRMED, result.status());
        assertEquals(now, result.confirmedAt());
        assertEquals(currentUserId, result.confirmedBy());
        assertEquals("Lễ Tân Nguyễn Văn A", result.confirmedByName());

        verify(appointmentRepository).save(appointment);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void rejectsConfirmationWhenAppointmentAlreadyCancelled_TC03() {
        Appointment appointment = Appointment.restore(
                appointmentId, "APT-002", patientId, doctorId,
                now.plusSeconds(3600), now.plusSeconds(5400),
                AppointmentStatus.CANCELLED, "Khám tổng quát",
                "Bận việc", null, null, UUID.randomUUID(), now.minusSeconds(7200)
        );

        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));

        assertThrows(AppointmentInvalidStatusException.class, () -> service.confirm(appointmentId));
        assertEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
        verify(appointmentRepository, never()).save(appointment);
    }

    @Test
    void rejectsConfirmationWhenAppointmentPastCutoff() {
        Appointment appointment = Appointment.restore(
                appointmentId, "APT-003", patientId, doctorId,
                now.minusSeconds(60), now.plusSeconds(1800),
                AppointmentStatus.SCHEDULED, "Khám tổng quát",
                null, null, null, UUID.randomUUID(), now.minusSeconds(7200)
        );

        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));

        assertThrows(AppointmentPastCutoffException.class, () -> service.confirm(appointmentId));
        assertEquals(AppointmentStatus.SCHEDULED, appointment.getStatus());
        verify(appointmentRepository, never()).save(appointment);
    }

    @Test
    void rejectsConfirmationWhenUserLacksReceptionistOrAdminRole_QTN01() {
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(false);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        assertThrows(UnauthorizedAppointmentOperationException.class, () -> service.confirm(appointmentId));

        verify(accessDeniedAuditWriter).writeConfirmDenied(eq(currentUserId), eq(appointmentId), eq(now), any());
        verify(appointmentRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void throwsNotFoundWhenAppointmentDoesNotExist() {
        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.empty());

        assertThrows(AppointmentNotFoundException.class, () -> service.confirm(appointmentId));
    }

    @Test
    void rejectsConfirmationWhenAppointmentAlreadyConfirmed_DoubleConfirmation() {
        Appointment appointment = Appointment.restore(
                appointmentId, "APT-004", patientId, doctorId,
                now.plusSeconds(3600), now.plusSeconds(5400),
                AppointmentStatus.CONFIRMED, "Khám tổng quát",
                null, null, null, UUID.randomUUID(), now.minusSeconds(7200),
                null, now.minusSeconds(300), currentUserId
        );

        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));

        assertThrows(AppointmentInvalidStatusException.class, () -> service.confirm(appointmentId));
        assertEquals(AppointmentStatus.CONFIRMED, appointment.getStatus());
        verify(appointmentRepository, never()).save(appointment);
    }
}
