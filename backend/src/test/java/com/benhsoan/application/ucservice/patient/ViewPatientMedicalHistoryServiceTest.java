package com.benhsoan.application.ucservice.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import com.benhsoan.application.ucservice.medicalrecord.MedicalRecordAccessAuditService;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.port.dto.command.patient.GetPatientMedicalHistoryQuery;
import com.benhsoan.port.dto.result.MedicalHistoryItemResult;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.patient.PatientMedicalHistoryQueryPort;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class ViewPatientMedicalHistoryServiceTest {

    @Mock
    private PatientRepository patientRepository;
    @Mock
    private PatientMedicalHistoryQueryPort patientMedicalHistoryQueryPort;
    @Mock
    private MedicalRecordAccessAuditService medicalRecordAccessAuditService;
    @Mock
    private CurrentUserPort currentUserPort;
    @Mock
    private ClockPort clockPort;
    @InjectMocks
    private ViewPatientMedicalHistoryService service;

    @Test
    void returnsHistoryAndRecordsSuccessfulAccess() {
        UUID patientId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-20T02:00:00Z");
        GetPatientMedicalHistoryQuery query = new GetPatientMedicalHistoryQuery(patientId, null, null, 0, 20);
        Page<MedicalHistoryItemResult> expected = new PageImpl<>(java.util.List.of());
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(org.mockito.Mockito.mock(Patient.class)));
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(patientMedicalHistoryQueryPort.findMedicalHistory(query)).thenReturn(expected);
        when(currentUserPort.getCurrentUserId()).thenReturn(currentUserId);
        when(clockPort.now()).thenReturn(now);

        Page<MedicalHistoryItemResult> result = service.viewMedicalHistory(query);

        assertEquals(expected, result);
        verify(medicalRecordAccessAuditService).recordHistoryView(patientId, currentUserId, now);
    }

    @Test
    void rejectsUnauthorizedUserWithoutQueryingOrAuditing() {
        UUID patientId = UUID.randomUUID();
        GetPatientMedicalHistoryQuery query = new GetPatientMedicalHistoryQuery(patientId, null, null, 0, 20);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(org.mockito.Mockito.mock(Patient.class)));
        when(currentUserPort.hasRole(any())).thenReturn(false);

        assertThrows(MedicalRecordAccessDeniedException.class, () -> service.viewMedicalHistory(query));

        verify(patientMedicalHistoryQueryPort, never()).findMedicalHistory(any());
        verify(medicalRecordAccessAuditService, never()).recordHistoryView(any(), any(), any());
    }

    @Test
    void rejectsMissingPatientBeforeQueryingOrAuditing() {
        UUID patientId = UUID.randomUUID();
        GetPatientMedicalHistoryQuery query = new GetPatientMedicalHistoryQuery(patientId, null, null, 0, 20);
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class, () -> service.viewMedicalHistory(query));

        verify(patientMedicalHistoryQueryPort, never()).findMedicalHistory(any());
        verify(medicalRecordAccessAuditService, never()).recordHistoryView(eq(patientId), any(), any());
    }
}
