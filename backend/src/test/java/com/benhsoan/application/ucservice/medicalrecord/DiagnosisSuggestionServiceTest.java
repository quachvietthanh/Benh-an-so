package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.result.DiagnosisSuggestionResult;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

@DisplayName("DiagnosisSuggestionService Tests")
@ExtendWith(MockitoExtension.class)
class DiagnosisSuggestionServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-25T00:00:00Z");
    private static final UUID DOCTOR = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
    private static final UUID SPECIALTY = UUID.fromString("f0000000-0000-0000-0000-000000000002");

    @Mock
    private DiagnosisCatalogRepository diagnosisCatalogRepository;
    @Mock
    private MedicalRecordDiagnosisRepository medicalRecordDiagnosisRepository;
    @Mock
    private VisitRepository visitRepository;
    @Mock
    private CurrentUserPort currentUserPort;
    @Spy
    private DiagnosisCatalogResultMapper resultMapper = new DiagnosisCatalogResultMapper();

    @InjectMocks
    private DiagnosisSuggestionService service;

    private DiagnosisCatalog catalog(UUID id, String code, String name, String group) {
        return DiagnosisCatalog.restore(id, code, name, group, null, true, NOW, null);
    }

    private Visit visit(UUID specialtyId) {
        return Visit.restore(UUID.randomUUID(), "VIS-001", UUID.randomUUID(), DOCTOR, null, null, specialtyId,
                VisitType.WALK_IN, VisitStatus.COMPLETED, NOW, NOW, NOW, "Exam", null, DOCTOR, NOW, null);
    }

    @Test
    @DisplayName("Returns recent codes for the current doctor only")
    void recentIsScopedToCurrentDoctor() {
        UUID catalogId = UUID.randomUUID();
        when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR);
        when(diagnosisCatalogRepository.findAllByActive(true))
                .thenReturn(List.of(catalog(catalogId, "J02.9", "Viêm họng cấp", "Hệ hô hấp")));
        when(medicalRecordDiagnosisRepository.findRecentCatalogIdsByDoctor(DOCTOR, 10)).thenReturn(List.of(catalogId));
        when(visitRepository.findMostRecentByDoctor(DOCTOR)).thenReturn(Optional.empty());

        DiagnosisSuggestionResult result = service.suggest();

        assertEquals(1, result.recent().size());
        assertEquals("J02.9", result.recent().getFirst().code());
        verify(medicalRecordDiagnosisRepository).findRecentCatalogIdsByDoctor(DOCTOR, 10);
    }

    @Test
    @DisplayName("Excludes inactive catalog entries from recent suggestions")
    void recentExcludesInactiveCodes() {
        UUID catalogId = UUID.randomUUID();
        when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR);
        when(diagnosisCatalogRepository.findAllByActive(true)).thenReturn(List.of());
        when(medicalRecordDiagnosisRepository.findRecentCatalogIdsByDoctor(DOCTOR, 10)).thenReturn(List.of(catalogId));
        when(visitRepository.findMostRecentByDoctor(DOCTOR)).thenReturn(Optional.empty());

        DiagnosisSuggestionResult result = service.suggest();

        assertTrue(result.recent().isEmpty());
    }

    @Test
    @DisplayName("Returns empty recent history for a doctor without diagnoses")
    void recentEmptyForDoctorWithoutHistory() {
        when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR);
        when(diagnosisCatalogRepository.findAllByActive(true)).thenReturn(List.of());
        when(medicalRecordDiagnosisRepository.findRecentCatalogIdsByDoctor(DOCTOR, 10)).thenReturn(List.of());
        when(visitRepository.findMostRecentByDoctor(DOCTOR)).thenReturn(Optional.empty());

        DiagnosisSuggestionResult result = service.suggest();

        assertTrue(result.recent().isEmpty());
        assertTrue(result.popular().isEmpty());
    }

    @Test
    @DisplayName("Builds popular codes from the doctor's most recent visit specialty")
    void popularDerivesFromRecentVisitSpecialty() {
        UUID popularCatalogId = UUID.randomUUID();
        when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR);
        when(diagnosisCatalogRepository.findAllByActive(true))
                .thenReturn(List.of(catalog(popularCatalogId, "I10", "Tăng huyết áp", "Hệ tuần hoàn")));
        when(visitRepository.findMostRecentByDoctor(DOCTOR)).thenReturn(Optional.of(visit(SPECIALTY)));
        when(medicalRecordDiagnosisRepository.findPopularCatalogIdsBySpecialty(SPECIALTY, 10))
                .thenReturn(List.of(popularCatalogId));

        DiagnosisSuggestionResult result = service.suggest();

        assertEquals(1, result.popular().size());
        assertEquals("I10", result.popular().getFirst().code());
        verify(medicalRecordDiagnosisRepository).findPopularCatalogIdsBySpecialty(SPECIALTY, 10);
    }

    @Test
    @DisplayName("Returns empty popular codes when the doctor has no visit")
    void popularEmptyWithoutVisit() {
        when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR);
        when(diagnosisCatalogRepository.findAllByActive(true)).thenReturn(List.of());
        when(medicalRecordDiagnosisRepository.findRecentCatalogIdsByDoctor(DOCTOR, 10)).thenReturn(List.of());
        when(visitRepository.findMostRecentByDoctor(DOCTOR)).thenReturn(Optional.empty());

        DiagnosisSuggestionResult result = service.suggest();

        assertTrue(result.popular().isEmpty());
        verify(medicalRecordDiagnosisRepository, never()).findPopularCatalogIdsBySpecialty(any(), anyInt());
    }

    @Test
    @DisplayName("Returns distinct sorted disease groups from active catalog entries")
    void returnsDistinctSortedDiseaseGroups() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR);
        when(diagnosisCatalogRepository.findAllByActive(true))
                .thenReturn(List.of(
                        catalog(id1, "I10", "Tăng huyết áp", "Hệ tuần hoàn"),
                        catalog(id2, "J00", "Cảm lạnh", "Hệ hô hấp"),
                        catalog(UUID.randomUUID(), "I48", "Rung nhĩ", "Hệ tuần hoàn")));
        when(medicalRecordDiagnosisRepository.findRecentCatalogIdsByDoctor(DOCTOR, 10)).thenReturn(List.of());
        when(visitRepository.findMostRecentByDoctor(DOCTOR)).thenReturn(Optional.empty());

        DiagnosisSuggestionResult result = service.suggest();

        assertEquals(List.of("Hệ hô hấp", "Hệ tuần hoàn"), result.diseaseGroups());
    }
}
