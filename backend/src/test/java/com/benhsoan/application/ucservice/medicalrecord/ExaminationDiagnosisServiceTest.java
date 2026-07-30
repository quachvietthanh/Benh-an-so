package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.command.medicalrecord.RecordDiagnosisCommand;
import com.benhsoan.port.dto.result.ExaminationDiagnosisResult;
import com.benhsoan.port.outbound.repository.crudRepository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.repository.crudRepository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

@DisplayName("ExaminationDiagnosisService Tests")
@ExtendWith(MockitoExtension.class)
class ExaminationDiagnosisServiceTest {

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private DiagnosisCatalogRepository diagnosisCatalogRepository;

    @Mock
    private CurrentUserPort currentUserPort;

    @InjectMocks
    private ExaminationDiagnosisService service;

    private final UUID examinationId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();

    private Visit activeVisit() {
        return Visit.restore(examinationId, "VIS000001", patientId, doctorId, null, null,
                VisitType.APPOINTMENT, VisitStatus.IN_PROGRESS,
                Instant.now(), Instant.now(), null,
                "Reason", null, doctorId, Instant.now(), null);
    }

    private Visit completedVisit() {
        return Visit.restore(examinationId, "VIS000001", patientId, doctorId, null, null,
                VisitType.APPOINTMENT, VisitStatus.COMPLETED,
                Instant.now(), Instant.now(), Instant.now(),
                "Reason", null, doctorId, Instant.now(), Instant.now());
    }

    private RecordDiagnosisCommand sampleCommand() {
        return new RecordDiagnosisCommand(
                UUID.randomUUID(), "J00", "Common cold", null, "Clinical notes"
        );
    }

    @Test
    @DisplayName("Should record diagnosis on active examination")
    void recordDiagnosisOnActiveVisit() {
        when(visitRepository.findById(examinationId)).thenReturn(Optional.of(activeVisit()));
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorId);

        ExaminationDiagnosisResult result = service.recordDiagnosis(examinationId, sampleCommand());

        assertNotNull(result);
        assertEquals(examinationId, result.visitId());
        assertEquals(doctorId, result.doctorId());
        assertEquals("J00", result.primaryIcdCode());
        assertEquals("Common cold", result.primaryIcdName());
        verify(visitRepository).findById(examinationId);
    }

    @Test
    @DisplayName("QTN-07: Should throw when visit is completed")
    void recordDiagnosisOnCompletedVisit() {
        when(visitRepository.findById(examinationId)).thenReturn(Optional.of(completedVisit()));

        assertThrows(ValidationException.class,
                () -> service.recordDiagnosis(examinationId, sampleCommand()));
    }

    @Test
    @DisplayName("Should throw when visit not found")
    void recordDiagnosisVisitNotFound() {
        when(visitRepository.findById(examinationId)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> service.recordDiagnosis(examinationId, sampleCommand()));
    }

    @Test
    @DisplayName("Should get diagnosis for existing examination")
    void getDiagnosisReturnsResult() {
        when(visitRepository.findById(examinationId)).thenReturn(Optional.of(activeVisit()));

        ExaminationDiagnosisResult result = service.getDiagnosis(examinationId);

        assertNotNull(result);
        assertEquals(examinationId, result.visitId());
        assertEquals(doctorId, result.doctorId());
        verify(visitRepository).findById(examinationId);
    }

    @Test
    @DisplayName("Should throw when get diagnosis for non-existent visit")
    void getDiagnosisNotFound() {
        when(visitRepository.findById(examinationId)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> service.getDiagnosis(examinationId));
    }
}
