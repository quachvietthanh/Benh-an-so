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

    private Appointment appointment(UUID appointmentId, UUID patientId) {
        return Appointment.restore(appointmentId, "APT000100", patientId, UUID.randomUUID(),
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
    void returnsOwnedAppointmentDetail() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment(appointmentId, patientId)));
        Patient authorized = patientWithId(patientId);
        when(patientAccessGuard.requirePatientOwnership(patientId, ResourceType.APPOINTMENT, appointmentId))
                .thenReturn(authorized);

        var result = service.getAppointmentDetail(appointmentId, null);

        assertEquals(appointmentId, result.id());
        verify(patientAccessGuard)
                .requirePatientOwnership(patientId, ResourceType.APPOINTMENT, appointmentId);
    }

    @Test
    void rejectsCrossPatientAccessWithForbidden() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment(appointmentId, patientId)));
        when(patientAccessGuard.requirePatientOwnership(patientId, ResourceType.APPOINTMENT, appointmentId))
                .thenThrow(new AccessDeniedException("Patient may only access their own data."));

        assertThrows(AccessDeniedException.class, () -> service.getAppointmentDetail(appointmentId, null));
    }

    @Test
    void throwsWhenAppointmentNotFound() {
        UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        assertThrows(AppointmentNotFoundException.class,
                () -> service.getAppointmentDetail(appointmentId, null));
    }

    // NCL-14-CN-010 -----------------------------------------------------

    @Test
    void returnsDependentAppointmentDetailWhenScopeAuthorised() {
        UUID appointmentId = UUID.randomUUID();
        UUID dependentId = UUID.randomUUID();

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment(appointmentId, dependentId)));
        Patient authorized = patientWithId(dependentId);
        when(patientAccessGuard.requirePatientAccess(dependentId, ResourceType.APPOINTMENT, appointmentId))
                .thenReturn(authorized);

        var result = service.getAppointmentDetail(appointmentId, dependentId);

        assertEquals(appointmentId, result.id());
        verify(patientAccessGuard)
                .requirePatientAccess(dependentId, ResourceType.APPOINTMENT, appointmentId);
    }

    @Test
    void rejectsDetailOfUnrelatedPatient() {
        UUID appointmentId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment(appointmentId, strangerId)));
        when(patientAccessGuard.requirePatientAccess(strangerId, ResourceType.APPOINTMENT, appointmentId))
                .thenThrow(new AccessDeniedException("Patient may only access their own data."));

        assertThrows(AccessDeniedException.class,
                () -> service.getAppointmentDetail(appointmentId, strangerId));
    }

    @Test
    void rejectsIdorWhenSuppliedPatientIdDiffersFromAppointmentOwner() {
        UUID appointmentId = UUID.randomUUID();
        UUID ownPatientId = UUID.randomUUID();
        UUID dependentId = UUID.randomUUID();

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment(appointmentId, ownPatientId)));
        Patient authorized = patientWithId(dependentId);
        when(patientAccessGuard.requirePatientAccess(dependentId, ResourceType.APPOINTMENT, appointmentId))
                .thenReturn(authorized);

        assertThrows(AccessDeniedException.class,
                () -> service.getAppointmentDetail(appointmentId, dependentId));

        verify(patientAccessGuard)
                .denyPatientAccess(ownPatientId, ResourceType.APPOINTMENT, appointmentId);
    }

    @Test
    void rejectsOwnScopeUsedToReachAThirdPartyAppointment() {
        UUID appointmentId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        UUID ownPatientId = UUID.randomUUID();

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment(appointmentId, strangerId)));
        Patient authorized = patientWithId(ownPatientId);
        when(patientAccessGuard.requirePatientAccess(ownPatientId, ResourceType.APPOINTMENT, appointmentId))
                .thenReturn(authorized);

        assertThrows(AccessDeniedException.class,
                () -> service.getAppointmentDetail(appointmentId, ownPatientId));

        verify(patientAccessGuard)
                .denyPatientAccess(strangerId, ResourceType.APPOINTMENT, appointmentId);
    }
}
