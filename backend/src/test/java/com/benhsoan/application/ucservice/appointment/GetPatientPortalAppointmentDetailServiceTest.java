package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

/**
 * NCL-14-CN-004 / NCL-14-CN-010: appointment detail authorization.
 *
 * The appointment's stored patientId is authoritative. The endpoint never requires a
 * client-supplied patientId, a supplied one can never widen access, and a nonexistent
 * appointment is externally indistinguishable from one outside the caller's scope, so the
 * endpoint is not an appointment existence oracle (P3.1 / P3.2).
 */
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

    private Patient authorizedPatient() {
        // The service no longer reads getId() from the resolved patient: authorization is
        // decided inside the guard from the appointment's own patientId (P3.1).
        return mock(Patient.class);
    }

    // --- P3.1: no patientId query parameter required --------------------

    @Test
    @DisplayName("P3.1: chu so huu xem chi tiet KHONG can patientId")
    void returnsOwnedAppointmentDetail() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment(appointmentId, patientId)));
        Patient authorized = authorizedPatient();
        when(patientAccessGuard.requirePatientAccess(patientId, ResourceType.APPOINTMENT, appointmentId))
                .thenReturn(authorized);

        var result = service.getAppointmentDetail(appointmentId, null);

        assertEquals(appointmentId, result.id());
        // Scope is derived from the appointment itself, not from a request parameter.
        verify(patientAccessGuard)
                .requirePatientAccess(patientId, ResourceType.APPOINTMENT, appointmentId);
    }

    @Test
    @DisplayName("P3.1: nguoi giam ho xem chi tiet KHONG can patientId")
    void guardianCanViewDependentAppointmentWithoutPatientId() {
        UUID appointmentId = UUID.randomUUID();
        UUID dependentId = UUID.randomUUID();

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment(appointmentId, dependentId)));
        Patient authorized = authorizedPatient();
        when(patientAccessGuard.requirePatientAccess(dependentId, ResourceType.APPOINTMENT, appointmentId))
                .thenReturn(authorized);

        var result = service.getAppointmentDetail(appointmentId, null);

        assertEquals(appointmentId, result.id());
        verify(patientAccessGuard)
                .requirePatientAccess(dependentId, ResourceType.APPOINTMENT, appointmentId);
    }

    @Test
    @DisplayName("P3.1: patientId khop voi lich hen thi giu nguyen hanh vi cu")
    void matchingPatientIdIsAccepted() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment(appointmentId, patientId)));
        Patient authorized = authorizedPatient();
        when(patientAccessGuard.requirePatientAccess(patientId, ResourceType.APPOINTMENT, appointmentId))
                .thenReturn(authorized);

        var result = service.getAppointmentDetail(appointmentId, patientId);

        assertEquals(appointmentId, result.id());
    }

    // --- P3.2: no appointment existence oracle --------------------------

    @Test
    @DisplayName("P3.2: lich hen khong ton tai va lich hen ngoai pham vi tra ve CUNG mot loi")
    void nonexistentAndUnauthorisedAreIndistinguishable() {
        UUID missingId = UUID.randomUUID();
        UUID foreignId = UUID.randomUUID();
        UUID foreignPatientId = UUID.randomUUID();

        when(appointmentRepository.findById(missingId)).thenReturn(Optional.empty());
        when(appointmentRepository.findById(foreignId))
                .thenReturn(Optional.of(appointment(foreignId, foreignPatientId)));
        when(patientAccessGuard.requirePatientAccess(
                foreignPatientId, ResourceType.APPOINTMENT, foreignId))
                .thenThrow(new AccessDeniedException("Patient may only access their own data."));

        AppointmentNotFoundException missing = assertThrows(AppointmentNotFoundException.class,
                () -> service.getAppointmentDetail(missingId, null));
        AppointmentNotFoundException unauthorised = assertThrows(AppointmentNotFoundException.class,
                () -> service.getAppointmentDetail(foreignId, null));

        // Identical externally observable result: existence cannot be told from ownership.
        assertEquals(missing.getCode(), unauthorised.getCode());
        assertEquals(missing.getMessage().replace(missingId.toString(), "X"),
                unauthorised.getMessage().replace(foreignId.toString(), "X"));
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
    @DisplayName("P3.1: patientId gui len KHONG khop lich hen bi tu choi + ghi nhat ky tu choi")
    void rejectsIdorWhenSuppliedPatientIdDiffersFromAppointmentOwner() {
        UUID appointmentId = UUID.randomUUID();
        UUID realOwnerId = UUID.randomUUID();
        UUID claimedId = UUID.randomUUID();

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment(appointmentId, realOwnerId)));

        assertThrows(AppointmentNotFoundException.class,
                () -> service.getAppointmentDetail(appointmentId, claimedId));

        verify(patientAccessGuard)
                .denyPatientAccess(realOwnerId, ResourceType.APPOINTMENT, appointmentId);
        // Scope is never even resolved for a mismatching claim.
        verify(patientAccessGuard, never()).requirePatientAccess(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("P3.1: khong the dung pham vi cua nguoi khac de doc lich hen nguoi thu ba")
    void rejectsOwnScopeUsedToReachAThirdPartyAppointment() {
        UUID appointmentId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        UUID ownPatientId = UUID.randomUUID();

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment(appointmentId, strangerId)));

        assertThrows(AppointmentNotFoundException.class,
                () -> service.getAppointmentDetail(appointmentId, ownPatientId));

        verify(patientAccessGuard)
                .denyPatientAccess(strangerId, ResourceType.APPOINTMENT, appointmentId);
    }
}
