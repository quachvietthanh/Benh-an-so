package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordSigningReminder;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.result.SigningReminderResult;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordSigningReminderRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetSigningRemindersService - Unit Tests (P1 BOLA Hardening)")
class GetSigningRemindersServiceTest {

    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private MedicalRecordSigningReminderRepository reminderRepository;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private MedicalRecordAuthorizationAuditService authorizationAuditService;

    @InjectMocks private GetSigningRemindersService service;

    private final UUID recordId = UUID.randomUUID();
    private final UUID visitId = UUID.randomUUID();
    private final UUID doctorAId = UUID.randomUUID();
    private final UUID doctorBId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-16T12:00:00Z");

    private MedicalRecord createDraftRecord() {
        return MedicalRecord.restore(
                recordId, visitId, "Headache", "Fever", null, null, null, null, null, null,
                MedicalRecordStatus.DRAFT, null, null, null, null, null, doctorBId, now.minus(Duration.ofHours(30)), null, null
        );
    }

    private Visit createVisitForDoctor(UUID docId) {
        return Visit.restore(
                visitId, "KB-001", patientId, docId, null, null,
                VisitType.APPOINTMENT, VisitStatus.COMPLETED,
                now.minus(Duration.ofHours(31)), now.minus(Duration.ofHours(30)), now.minus(Duration.ofHours(30)),
                "Headache", "Note", docId, now.minus(Duration.ofHours(30)), now.minus(Duration.ofHours(30))
        );
    }

    @Test
    @DisplayName("P1 BOLA: Doctor A viewing Doctor B record reminders is denied and audit logged")
    void doctorAViewingDoctorBRecordRemindersDeniedAndAuditLogged() {
        MedicalRecord record = createDraftRecord();
        Visit visitOfDoctorB = createVisitForDoctor(doctorBId);

        when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(false);
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorAId);
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visitOfDoctorB));

        assertThrows(MedicalRecordAccessDeniedException.class, () -> service.getReminders(recordId));

        verify(authorizationAuditService).recordTemplateAccessDenied(
                eq(doctorAId),
                eq(recordId),
                anyString()
        );
        verifyNoInteractions(reminderRepository);
    }

    @Test
    @DisplayName("Doctor viewing own record reminders is allowed")
    void doctorViewingOwnRecordRemindersAllowed() {
        MedicalRecord record = createDraftRecord();
        Visit visitOfDoctorB = createVisitForDoctor(doctorBId);

        when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(false);
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorBId);
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visitOfDoctorB));

        MedicalRecordSigningReminder reminder = MedicalRecordSigningReminder.create(
                recordId, doctorBId, UUID.randomUUID(), now, 6L, "SYSTEM", "Nhắc ký"
        );
        when(reminderRepository.findByMedicalRecordId(recordId)).thenReturn(List.of(reminder));
        when(userRepository.findAllById(any())).thenReturn(List.of());

        List<SigningReminderResult> results = service.getReminders(recordId);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(doctorBId, results.get(0).doctorId());
        verifyNoInteractions(authorizationAuditService);
    }

    @Test
    @DisplayName("Manager viewing any doctor record reminders is allowed")
    void managerViewingAnyRecordRemindersAllowed() {
        MedicalRecord record = createDraftRecord();

        when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(false);

        MedicalRecordSigningReminder reminder = MedicalRecordSigningReminder.create(
                recordId, doctorBId, UUID.randomUUID(), now, 6L, "SYSTEM", "Nhắc ký"
        );
        when(reminderRepository.findByMedicalRecordId(recordId)).thenReturn(List.of(reminder));
        when(userRepository.findAllById(any())).thenReturn(List.of());

        List<SigningReminderResult> results = service.getReminders(recordId);

        assertNotNull(results);
        assertEquals(1, results.size());
        verifyNoInteractions(authorizationAuditService);
    }

    @Test
    @DisplayName("Non-existent medical record throws MedicalRecordNotFoundException")
    void nonExistentRecordThrows() {
        when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.empty());

        assertThrows(MedicalRecordNotFoundException.class, () -> service.getReminders(recordId));
    }
}
