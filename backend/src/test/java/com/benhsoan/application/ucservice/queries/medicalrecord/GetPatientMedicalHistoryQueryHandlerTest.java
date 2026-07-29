package com.benhsoan.application.ucservice.queries.medicalrecord;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.Visit;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.patient.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.port.dto.command.patient.GetPatientMedicalHistoryQuery;
import com.benhsoan.port.outbound.repository.crudRepository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.crudRepository.patient.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

@DisplayName("GetPatientMedicalHistoryQueryHandler - Permission Tests")
@ExtendWith(MockitoExtension.class)
class GetPatientMedicalHistoryQueryHandlerTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private CurrentUserPort currentUserPort;

    @Mock
    private MedicalRecordAuditService auditService;

    @InjectMocks
    private GetPatientMedicalHistoryQueryHandler handler;

    private final UUID currentUserId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();

    private Patient createPatient(UUID userId) {
        return Patient.restore(
                patientId,
                "P001",
                "Test Patient",
                LocalDate.of(1990, 1, 1),
                Gender.MALE, null, null, null,
                null, null, null, null, null,
                true, Instant.now(), Instant.now(),
                userId, UUID.randomUUID()
        );
    }

    private GetPatientMedicalHistoryQuery defaultQuery() {
        return GetPatientMedicalHistoryQuery.of(
                patientId, null, null, 0, 10
        );
    }

    private Page<Visit> emptyPage() {
        return new PageImpl<>(java.util.Collections.emptyList());
    }

    // ==================== ADMIN ====================

    @Test
    @DisplayName("ADMIN should be able to view any patient's history")
    void adminCanViewAnyHistory() {
        Patient patient = createPatient(otherUserId);
        when(patientRepository.findById(patientId))
                .thenReturn(Optional.of(patient));
        when(currentUserPort.getCurrentUserId())
                .thenReturn(currentUserId);
        when(currentUserPort.getCurrentUserRoles())
                .thenReturn(Set.of("ADMIN"));
        when(visitRepository.findByPatientIdWithDateFilter(
                eq(patientId), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage());
        doNothing().when(auditService).logViewHistory(any(), any());

        Page<Visit> result = handler.handle(defaultQuery());

        assertNotNull(result);
        verify(auditService).logViewHistory(patientId, currentUserId);
    }

    // ==================== PATIENT ====================

    @Test
    @DisplayName("PATIENT should be able to view their own history")
    void patientCanViewOwnHistory() {
        Patient patient = createPatient(currentUserId);
        when(patientRepository.findById(patientId))
                .thenReturn(Optional.of(patient));
        when(currentUserPort.getCurrentUserId())
                .thenReturn(currentUserId);
        when(currentUserPort.getCurrentUserRoles())
                .thenReturn(Set.of("PATIENT"));
        when(visitRepository.findByPatientIdWithDateFilter(
                eq(patientId), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage());
        doNothing().when(auditService).logViewHistory(any(), any());

        Page<Visit> result = handler.handle(defaultQuery());

        assertNotNull(result);
        verify(auditService).logViewHistory(patientId, currentUserId);
    }

    @Test
    @DisplayName("PATIENT should be denied when viewing another patient's history")
    void patientCannotViewOtherHistory() {
        Patient patient = createPatient(otherUserId);
        when(patientRepository.findById(patientId))
                .thenReturn(Optional.of(patient));
        when(currentUserPort.getCurrentUserId())
                .thenReturn(currentUserId);
        when(currentUserPort.getCurrentUserRoles())
                .thenReturn(Set.of("PATIENT"));

        assertThrows(MedicalRecordAccessDeniedException.class,
                () -> handler.handle(defaultQuery()));

        verify(auditService).logAccessDenied(
                patientId, currentUserId, "Insufficient permissions");
    }

    @Test
    @DisplayName("PATIENT without userId on Patient record should be denied")
    void patientWithNullUserIdIsDenied() {
        Patient patient = createPatient(null);
        when(patientRepository.findById(patientId))
                .thenReturn(Optional.of(patient));
        when(currentUserPort.getCurrentUserId())
                .thenReturn(currentUserId);
        when(currentUserPort.getCurrentUserRoles())
                .thenReturn(Set.of("PATIENT"));

        assertThrows(MedicalRecordAccessDeniedException.class,
                () -> handler.handle(defaultQuery()));

        verify(auditService).logAccessDenied(
                patientId, currentUserId, "Insufficient permissions");
    }

    // ==================== DOCTOR ====================

    @Test
    @DisplayName("DOCTOR who treated the patient should be able to view history")
    void doctorWhoTreatedPatientCanViewHistory() {
        Patient patient = createPatient(otherUserId);
        when(patientRepository.findById(patientId))
                .thenReturn(Optional.of(patient));
        when(currentUserPort.getCurrentUserId())
                .thenReturn(currentUserId);
        when(currentUserPort.getCurrentUserRoles())
                .thenReturn(Set.of("DOCTOR"));
        when(visitRepository.existsByPatientIdAndDoctorId(
                patientId, currentUserId))
                .thenReturn(true);
        when(visitRepository.findByPatientIdWithDateFilter(
                eq(patientId), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage());
        doNothing().when(auditService).logViewHistory(any(), any());

        Page<Visit> result = handler.handle(defaultQuery());

        assertNotNull(result);
        verify(auditService).logViewHistory(patientId, currentUserId);
    }

    @Test
    @DisplayName("DOCTOR who never treated the patient should be denied")
    void doctorWhoNeverTreatedPatientIsDenied() {
        Patient patient = createPatient(otherUserId);
        when(patientRepository.findById(patientId))
                .thenReturn(Optional.of(patient));
        when(currentUserPort.getCurrentUserId())
                .thenReturn(currentUserId);
        when(currentUserPort.getCurrentUserRoles())
                .thenReturn(Set.of("DOCTOR"));
        when(visitRepository.existsByPatientIdAndDoctorId(
                patientId, currentUserId))
                .thenReturn(false);

        assertThrows(MedicalRecordAccessDeniedException.class,
                () -> handler.handle(defaultQuery()));

        verify(auditService).logAccessDenied(
                patientId, currentUserId, "Insufficient permissions");
    }

    // ==================== VALIDATION ====================

    @Test
    @DisplayName("fromDate after toDate should throw validation exception")
    void fromDateAfterToDateThrowsException() {
        Instant from = Instant.parse("2026-06-15T00:00:00Z");
        Instant to = Instant.parse("2026-01-01T00:00:00Z");

        assertThrows(IllegalArgumentException.class,
                () -> GetPatientMedicalHistoryQuery.of(
                        patientId, from, to, 0, 10));
    }

    // ==================== PATIENT NOT FOUND ====================

    @Test
    @DisplayName("Should throw PatientNotFoundException when patient does not exist")
    void patientNotFoundThrowsException() {
        when(patientRepository.findById(patientId))
                .thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class,
                () -> handler.handle(defaultQuery()));
    }
}
