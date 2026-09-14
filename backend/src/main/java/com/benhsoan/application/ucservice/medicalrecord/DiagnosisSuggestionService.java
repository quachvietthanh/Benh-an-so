package com.benhsoan.application.ucservice.medicalrecord;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.dto.result.DiagnosisCatalogResult;
import com.benhsoan.port.dto.result.DiagnosisSuggestionResult;
import com.benhsoan.port.inbound.medicalrecord.GetDiagnosisSuggestionsUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

/**
 * Builds doctor-scoped and specialty-scoped disease-code suggestions for the
 * current authenticated doctor. Recent codes come from the doctor's own
 * diagnosis history; popular codes come from an aggregate over the specialty of
 * the doctor's most recent visit. Both are restricted to active catalog
 * entries so suggestions stay consistent with the disease-code lookup.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiagnosisSuggestionService implements GetDiagnosisSuggestionsUseCase {

    private static final int RECENT_LIMIT = 10;
    private static final int POPULAR_LIMIT = 10;

    private final DiagnosisCatalogRepository diagnosisCatalogRepository;
    private final MedicalRecordDiagnosisRepository medicalRecordDiagnosisRepository;
    private final VisitRepository visitRepository;
    private final CurrentUserPort currentUserPort;
    private final DiagnosisCatalogResultMapper resultMapper;

    @Override
    public DiagnosisSuggestionResult suggest() {
        UUID doctorId = currentUserPort.getCurrentUserId();
        List<DiagnosisCatalog> activeCatalogs = diagnosisCatalogRepository.findAllByActive(true);
        Map<UUID, DiagnosisCatalog> activeById = activeCatalogs.stream()
                .collect(Collectors.toMap(DiagnosisCatalog::getId, catalog -> catalog));

        return new DiagnosisSuggestionResult(
                buildRecent(doctorId, activeById),
                buildPopular(doctorId, activeById),
                buildDiseaseGroups(activeCatalogs)
        );
    }

    private List<DiagnosisCatalogResult> buildRecent(UUID doctorId, Map<UUID, DiagnosisCatalog> activeById) {
        return medicalRecordDiagnosisRepository.findRecentCatalogIdsByDoctor(doctorId, RECENT_LIMIT).stream()
                .map(activeById::get)
                .filter(Objects::nonNull)
                .map(resultMapper::toResult)
                .toList();
    }

    private List<DiagnosisCatalogResult> buildPopular(UUID doctorId, Map<UUID, DiagnosisCatalog> activeById) {
        UUID specialtyId = visitRepository.findMostRecentByDoctor(doctorId)
                .map(Visit::getSpecialtyId)
                .orElse(null);
        if (specialtyId == null) {
            return List.of();
        }
        return medicalRecordDiagnosisRepository.findPopularCatalogIdsBySpecialty(specialtyId, POPULAR_LIMIT).stream()
                .map(activeById::get)
                .filter(Objects::nonNull)
                .map(resultMapper::toResult)
                .toList();
    }

    private List<String> buildDiseaseGroups(List<DiagnosisCatalog> activeCatalogs) {
        return activeCatalogs.stream()
                .map(DiagnosisCatalog::getDiseaseGroup)
                .filter(Objects::nonNull)
                .filter(group -> !group.isBlank())
                .distinct()
                .sorted()
                .toList();
    }
}
