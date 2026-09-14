package com.benhsoan.application.ucservice.medicalrecord;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogNotFoundException;
import com.benhsoan.domain.shared.VietnameseTextNormalizer;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.DiagnosisCatalogResult;
import com.benhsoan.port.inbound.medicalrecord.DiagnosisCatalogManagementQueryUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetDiagnosisCatalogUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;

import lombok.RequiredArgsConstructor;

/**
 * Doctor-facing and admin-facing diagnosis catalog queries. The doctor-facing
 * search is pushed into the database (bounded and accent-insensitive via
 * persisted normalized columns); no in-memory ranking is performed here.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiagnosisCatalogService implements GetDiagnosisCatalogUseCase, DiagnosisCatalogManagementQueryUseCase {

    private static final int MAX_RESULTS = 50;

    private final DiagnosisCatalogRepository repository;
    private final DiagnosisCatalogResultMapper resultMapper;

    @Override
    public List<DiagnosisCatalogResult> search(String query, String diseaseGroup) {
        String keyword = normalizeKeyword(query);
        String group = normalizeGroup(diseaseGroup);
        if (keyword == null) {
            if (group == null) {
                return Collections.emptyList();
            }
            return repository.findByActiveAndDiseaseGroup(group, MAX_RESULTS).stream()
                    .map(resultMapper::toResult)
                    .toList();
        }
        return repository.searchActive(keyword, group, MAX_RESULTS).stream()
                .map(resultMapper::toResult)
                .toList();
    }

    private String normalizeKeyword(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String normalized = VietnameseTextNormalizer.normalize(query);
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeGroup(String diseaseGroup) {
        if (diseaseGroup == null || diseaseGroup.isBlank()) {
            return null;
        }
        return diseaseGroup.trim();
    }

    @Override
    public List<DiagnosisCatalogResult> search(String keyword, Boolean active) {
        return repository.search(keyword, active).stream()
                .map(resultMapper::toResult)
                .toList();
    }

    @Override
    public DiagnosisCatalogResult getById(UUID diagnosisCatalogId) {
        if (diagnosisCatalogId == null) {
            throw new ValidationException("Diagnosis catalog id is required.");
        }
        return repository.findById(diagnosisCatalogId)
                .map(resultMapper::toResult)
                .orElseThrow(() -> new DiagnosisCatalogNotFoundException(diagnosisCatalogId));
    }
}
