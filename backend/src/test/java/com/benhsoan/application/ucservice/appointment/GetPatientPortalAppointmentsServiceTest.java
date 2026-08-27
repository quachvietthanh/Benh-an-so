package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.dto.result.appointment.PatientAppointmentResult;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

@ExtendWith(MockitoExtension.class)
class GetPatientPortalAppointmentsServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private CurrentUserPort currentUserPort;

    private GetPatientPortalAppointmentsService service;

    @BeforeEach
    void setUp() {
        service = new GetPatientPortalAppointmentsService(
                appointmentRepository,
                patientRepository,
                currentUserPort,
                new PatientAppointmentResultMapper()
        );
    }

    @Test
    void returnsActiveAppointmentsForOwnPatientByDefault() {
        UUID userId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(patientId);
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));

        Appointment appointment = Appointment.restore(UUID.randomUUID(), "APT000100", patientId, doctorId,
                Instant.parse("2099-08-10T02:00:00Z"), Instant.parse("2099-08-10T02:30:00Z"),
                AppointmentStatus.SCHEDULED, "Khám tổng quát", null, null, null,
                userId, Instant.parse("2026-08-01T00:00:00Z"));
        when(appointmentRepository.findByPatientIdAndStatusInOrderByStartTimeAsc(
                patientId, List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED)))
                .thenReturn(List.of(appointment));

        List<PatientAppointmentResult> results = service.getAppointments(null);

        assertEquals(1, results.size());
        assertEquals(patientId, results.get(0).patientId());
        verify(appointmentRepository).findByPatientIdAndStatusInOrderByStartTimeAsc(
                patientId, List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED));
    }

    @Test
    void honorsExplicitStatusFilter() {
        UUID userId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(patientId);
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));
        when(appointmentRepository.findByPatientIdAndStatusInOrderByStartTimeAsc(
                patientId, List.of(AppointmentStatus.CANCELLED))).thenReturn(List.of());

        service.getAppointments(AppointmentStatus.CANCELLED);

        verify(appointmentRepository).findByPatientIdAndStatusInOrderByStartTimeAsc(
                patientId, List.of(AppointmentStatus.CANCELLED));
    }

    @Test
    void rejectsWhenNoPatientProfileIsLinked() {
        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(patientRepository.findByUserId(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> service.getAppointments(null));
    }
}
