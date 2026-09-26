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

import com.benhsoan.application.ucservice.patient.PatientAccessGuard;
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
    @Mock private PatientAccessGuard patientAccessGuard;

    private GetPatientPortalAppointmentsService service;

    @BeforeEach
    void setUp() {
        service = new GetPatientPortalAppointmentsService(
                appointmentRepository,
                patientRepository,
                currentUserPort,
                patientAccessGuard,
                new PatientAppointmentResultMapper()
        );
    }

    private Appointment appointment(UUID patientId) {
        return Appointment.restore(UUID.randomUUID(), "APT000100", patientId, UUID.randomUUID(),
                Instant.parse("2099-08-10T02:00:00Z"), Instant.parse("2099-08-10T02:30:00Z"),
                AppointmentStatus.SCHEDULED, "Kham tong quat", null, null, null,
                UUID.randomUUID(), Instant.parse("2026-08-01T00:00:00Z"));
    }

    private Patient patientWithId(UUID patientId) {
        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(patientId);
        return patient;
    }

    @Test
    void returnsActiveAppointmentsForOwnPatientByDefault() {
        UUID userId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        Patient ownPatient = patientWithId(patientId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(ownPatient));
        when(appointmentRepository.findByPatientIdAndStatusInOrderByStartTimeAsc(
                patientId, List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED)))
                .thenReturn(List.of(appointment(patientId)));

        List<PatientAppointmentResult> results = service.getAppointments(null, null);

        assertEquals(1, results.size());
        assertEquals(patientId, results.get(0).patientId());
        verify(patientAccessGuard, org.mockito.Mockito.never()).requirePatientAccess(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void honorsExplicitStatusFilter() {
        UUID userId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        Patient ownPatient = patientWithId(patientId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(ownPatient));
        when(appointmentRepository.findByPatientIdAndStatusInOrderByStartTimeAsc(
                patientId, List.of(AppointmentStatus.CANCELLED))).thenReturn(List.of());

        service.getAppointments(AppointmentStatus.CANCELLED, null);

        verify(appointmentRepository).findByPatientIdAndStatusInOrderByStartTimeAsc(
                patientId, List.of(AppointmentStatus.CANCELLED));
    }

    @Test
    void rejectsWhenNoPatientProfileIsLinked() {
        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(patientRepository.findByUserId(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> service.getAppointments(null, null));
    }

    // NCL-14-CN-010 -----------------------------------------------------

    @Test
    void listsAppointmentsOfLinkedDependentWhenPatientIdSupplied() {
        UUID dependentId = UUID.randomUUID();

        Patient dependent = patientWithId(dependentId);
        when(patientAccessGuard.requirePatientAccess(dependentId)).thenReturn(dependent);
        when(appointmentRepository.findByPatientIdAndStatusInOrderByStartTimeAsc(
                dependentId, List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED)))
                .thenReturn(List.of(appointment(dependentId)));

        List<PatientAppointmentResult> results = service.getAppointments(null, dependentId);

        assertEquals(1, results.size());
        assertEquals(dependentId, results.get(0).patientId());
        verify(patientAccessGuard).requirePatientAccess(dependentId);
    }

    @Test
    void rejectsUnrelatedPatientScope() {
        UUID strangerId = UUID.randomUUID();

        when(patientAccessGuard.requirePatientAccess(strangerId))
                .thenThrow(new AccessDeniedException("Patient may only access their own data."));

        assertThrows(AccessDeniedException.class, () -> service.getAppointments(null, strangerId));

        verify(appointmentRepository, org.mockito.Mockito.never())
                .findByPatientIdAndStatusInOrderByStartTimeAsc(
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void familyScopeIsAuthorisedByTheGuardNotByTheRequestedId() {
        UUID dependentId = UUID.randomUUID();

        Patient dependent = patientWithId(dependentId);
        when(patientAccessGuard.requirePatientAccess(dependentId)).thenReturn(dependent);
        when(appointmentRepository.findByPatientIdAndStatusInOrderByStartTimeAsc(
                dependentId, List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED)))
                .thenReturn(List.of());

        service.getAppointments(null, dependentId);

        verify(patientRepository, org.mockito.Mockito.never())
                .findByUserId(org.mockito.ArgumentMatchers.any());
    }
}
