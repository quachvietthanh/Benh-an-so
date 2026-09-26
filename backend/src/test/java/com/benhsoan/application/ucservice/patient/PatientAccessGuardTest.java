package com.benhsoan.application.ucservice.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class PatientAccessGuardTest {

    private static final Instant NOW = Instant.parse("2026-08-26T02:00:00Z");

    @Mock private CurrentUserPort currentUserPort;
    @Mock private PatientRepository patientRepository;
    @Mock private PatientAccessDeniedAuditWriter denialAuditWriter;
    @Mock private ClockPort clockPort;

    @Test
    void matchingOwnershipReturnsOwnPatient() {
        UUID userId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        Patient own = mock(Patient.class);
        when(own.getId()).thenReturn(patientId);

        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(own));

        PatientAccessGuard guard = new PatientAccessGuard(currentUserPort, patientRepository, denialAuditWriter, clockPort);

        Patient result = guard.requirePatientOwnership(patientId);

        assertEquals(patientId, result.getId());
    }

    @Test
    void mismatchedOwnershipThrows403AndWritesDenialAudit() {
        UUID userId = UUID.randomUUID();
        UUID targetPatientId = UUID.randomUUID();

        Patient own = mock(Patient.class);
        when(own.getId()).thenReturn(UUID.randomUUID());

        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(own));
        when(clockPort.now()).thenReturn(NOW);

        PatientAccessGuard guard = new PatientAccessGuard(currentUserPort, patientRepository, denialAuditWriter, clockPort);

        assertThrows(AccessDeniedException.class, () -> guard.requirePatientOwnership(targetPatientId));

        verify(denialAuditWriter).writeDenied(userId, targetPatientId, NOW, ResourceType.PATIENT, targetPatientId);
    }

    @Test
    void noLinkedPatientThrows403AndWritesDenialAudit() {
        UUID userId = UUID.randomUUID();
        UUID targetPatientId = UUID.randomUUID();

        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(clockPort.now()).thenReturn(NOW);

        PatientAccessGuard guard = new PatientAccessGuard(currentUserPort, patientRepository, denialAuditWriter, clockPort);

        assertThrows(AccessDeniedException.class, () -> guard.requirePatientOwnership(targetPatientId));

        verify(denialAuditWriter).writeDenied(userId, targetPatientId, NOW, ResourceType.PATIENT, targetPatientId);
    }

    @Test
    void appointmentOwnershipMismatchWritesAppointmentDenialAudit() {
        UUID userId = UUID.randomUUID();
        UUID targetPatientId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();

        Patient own = mock(Patient.class);
        when(own.getId()).thenReturn(UUID.randomUUID());

        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(own));
        when(clockPort.now()).thenReturn(NOW);

        PatientAccessGuard guard = new PatientAccessGuard(currentUserPort, patientRepository, denialAuditWriter, clockPort);

        assertThrows(AccessDeniedException.class,
                () -> guard.requirePatientOwnership(targetPatientId, ResourceType.APPOINTMENT, appointmentId));

        verify(denialAuditWriter).writeDenied(userId, targetPatientId, NOW, ResourceType.APPOINTMENT, appointmentId);
    }

    // ------------------------------------------------------------------
    // NCL-14-CN-010 CV-02: family scope (own profile + explicitly linked dependent)
    // ------------------------------------------------------------------

    private PatientAccessGuard familyGuard() {
        return new PatientAccessGuard(currentUserPort, patientRepository, denialAuditWriter, clockPort);
    }

    @Test
    void requirePatientAccessAllowsOwnPatientWithoutDenialAudit() {
        UUID userId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        Patient own = mock(Patient.class);
        when(own.getId()).thenReturn(patientId);
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(own));

        Patient result = familyGuard().requirePatientAccess(patientId);

        assertEquals(patientId, result.getId());
        org.mockito.Mockito.verifyNoInteractions(denialAuditWriter);
    }

    @Test
    void requirePatientAccessAllowsLinkedDependent() {
        UUID userId = UUID.randomUUID();
        UUID ownPatientId = UUID.randomUUID();
        UUID dependentId = UUID.randomUUID();

        Patient own = mock(Patient.class);
        when(own.getId()).thenReturn(ownPatientId);
        Patient dependent = mock(Patient.class);
        when(dependent.getId()).thenReturn(dependentId);

        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(own));
        when(patientRepository.findByGuardianUserIdAndId(userId, dependentId))
                .thenReturn(Optional.of(dependent));

        Patient result = familyGuard().requirePatientAccess(dependentId);

        assertEquals(dependentId, result.getId());
        org.mockito.Mockito.verifyNoInteractions(denialAuditWriter);
    }

    @Test
    void requirePatientAccessDeniesUnrelatedPatientAndWritesDenialAudit() {
        UUID userId = UUID.randomUUID();
        UUID ownPatientId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();

        Patient own = mock(Patient.class);
        when(own.getId()).thenReturn(ownPatientId);

        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(own));
        when(patientRepository.findByGuardianUserIdAndId(userId, strangerId))
                .thenReturn(Optional.empty());
        when(clockPort.now()).thenReturn(NOW);

        assertThrows(AccessDeniedException.class, () -> familyGuard().requirePatientAccess(strangerId));

        verify(denialAuditWriter).writeDenied(userId, strangerId, NOW, ResourceType.PATIENT, strangerId);
    }

    @Test
    void requirePatientAccessDeniesWhenNoProfileLinkedAndWritesDenialAudit() {
        UUID userId = UUID.randomUUID();
        UUID targetPatientId = UUID.randomUUID();

        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(patientRepository.findByGuardianUserIdAndId(userId, targetPatientId))
                .thenReturn(Optional.empty());
        when(clockPort.now()).thenReturn(NOW);

        assertThrows(AccessDeniedException.class,
                () -> familyGuard().requirePatientAccess(targetPatientId));

        verify(denialAuditWriter).writeDenied(userId, targetPatientId, NOW, ResourceType.PATIENT, targetPatientId);
    }

    @Test
    void dependentLinkedToAnotherGuardianIsNotReachable() {
        UUID userId = UUID.randomUUID();
        UUID otherGuardianUserId = UUID.randomUUID();
        UUID dependentId = UUID.randomUUID();

        Patient own = mock(Patient.class);
        when(own.getId()).thenReturn(UUID.randomUUID());

        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(own));
        when(patientRepository.findByGuardianUserIdAndId(userId, dependentId))
                .thenReturn(Optional.empty());
        when(clockPort.now()).thenReturn(NOW);

        assertThrows(AccessDeniedException.class, () -> familyGuard().requirePatientAccess(dependentId));

        verify(denialAuditWriter).writeDenied(userId, dependentId, NOW, ResourceType.PATIENT, dependentId);
        org.mockito.Mockito.verify(patientRepository, org.mockito.Mockito.never())
                .findByGuardianUserIdAndId(otherGuardianUserId, dependentId);
    }

    @Test
    void requirePatientAccessReportsResourceScopedDenialAudit() {
        UUID userId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UUID dependentId = UUID.randomUUID();

        Patient own = mock(Patient.class);
        when(own.getId()).thenReturn(UUID.randomUUID());

        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(own));
        when(patientRepository.findByGuardianUserIdAndId(userId, dependentId))
                .thenReturn(Optional.empty());
        when(clockPort.now()).thenReturn(NOW);

        assertThrows(AccessDeniedException.class, () -> familyGuard()
                .requirePatientAccess(dependentId, ResourceType.APPOINTMENT, appointmentId));

        verify(denialAuditWriter)
                .writeDenied(userId, dependentId, NOW, ResourceType.APPOINTMENT, appointmentId);
    }

    @Test
    void requirePatientOwnershipStillDeniesALinkedDependent() {
        UUID userId = UUID.randomUUID();
        UUID dependentId = UUID.randomUUID();

        Patient own = mock(Patient.class);
        when(own.getId()).thenReturn(UUID.randomUUID());

        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(own));
        when(clockPort.now()).thenReturn(NOW);

        assertThrows(AccessDeniedException.class,
                () -> familyGuard().requirePatientOwnership(dependentId));

        verify(denialAuditWriter).writeDenied(userId, dependentId, NOW, ResourceType.PATIENT, dependentId);
    }

    @Test
    void denyPatientAccessWritesDenialAuditForTheResolvedOwner() {
        UUID userId = UUID.randomUUID();
        UUID ownerPatientId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();

        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(clockPort.now()).thenReturn(NOW);

        familyGuard().denyPatientAccess(ownerPatientId, ResourceType.APPOINTMENT, appointmentId);

        verify(denialAuditWriter)
                .writeDenied(userId, ownerPatientId, NOW, ResourceType.APPOINTMENT, appointmentId);
    }
}
