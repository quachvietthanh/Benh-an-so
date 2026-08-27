package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.exception.AppointmentNotFoundException;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;

@ExtendWith(MockitoExtension.class)
class GetPatientPortalAppointmentDetailServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PatientAccessGuard patientAccessGuard;

    private GetPatientPortalAppointmentDetailService service;

    @BeforeEach
    void setUp() {
        service = new GetPatientPortalAppointmentDetailService(
                appointmentRepository,
                patientAccessGuard,
                new PatientAppointmentResultMapper()
        );
    }

    @Test
    void returnsOwnedAppointmentDetail() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Appointment appointment = Appointment.restore(appointmentId, "APT000100", patientId, doctorId,
                Instant.parse("2099-08-10T02:00:00Z"), Instant.parse("2099-08-10T02:30:00Z"),
                AppointmentStatus.SCHEDULED, "Khám tổng quát", null, null, null,
                userId, Instant.parse("2026-08-01T00:00:00Z"));

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(patientAccessGuard.requirePatientOwnership(patientId, ResourceType.APPOINTMENT, appointmentId))
                .thenReturn(mock(Patient.class));

        var result = service.getAppointmentDetail(appointmentId);

        assertEquals(appointmentId, result.id());
        verify(patientAccessGuard).requirePatientOwnership(patientId, ResourceType.APPOINTMENT, appointmentId);
    }

    @Test
    void rejectsCrossPatientAccessWithForbidden() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Appointment appointment = Appointment.restore(appointmentId, "APT000101", patientId, doctorId,
                Instant.parse("2099-08-10T02:00:00Z"), Instant.parse("2099-08-10T02:30:00Z"),
                AppointmentStatus.SCHEDULED, "Khám tổng quát", null, null, null,
                userId, Instant.parse("2026-08-01T00:00:00Z"));

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(patientAccessGuard.requirePatientOwnership(patientId, ResourceType.APPOINTMENT, appointmentId))
                .thenThrow(new AccessDeniedException("Patient may only access their own data."));

        assertThrows(AccessDeniedException.class, () -> service.getAppointmentDetail(appointmentId));
    }

    @Test
    void throwsWhenAppointmentNotFound() {
        UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        assertThrows(AppointmentNotFoundException.class, () -> service.getAppointmentDetail(appointmentId));
    }
}
