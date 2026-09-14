package com.benhsoan.application.ucservice.medicalrecord;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogNotFoundException;
import com.benhsoan.domain.shared.VietnameseTextNormalizer;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.DiagnosisCatalogResult;
import com.benhsoan.port.inbound.medicalrecord.DiagnosisCatalogManagementQueryUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetDiagnosisCatalogUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;

import lombok.RequiredArgsConstructor;

/**
 * Step 5 (Simplicity Check): Keeping service simple - one method that delegates to repository with mapping.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiagnosisCatalogService implements GetDiagnosisCatalogUseCase, DiagnosisCatalogManagementQueryUseCase {

    private final DiagnosisCatalogRepository repository;
    private final DiagnosisCatalogResultMapper resultMapper;

    @Override
    public List<DiagnosisCatalogResult> search(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        String keyword = VietnameseTextNormalizer.normalize(query);
        if (keyword.isEmpty()) {
            return Collections.emptyList();
        }
        return repository.findAllByActive(true).stream()
                .map(catalog -> new RankedMatch(catalog, rank(catalog, keyword)))
                .filter(match -> match.rank() >= 0)
                .sorted(Comparator.comparingInt(RankedMatch::rank)
                        .thenComparing(match -> match.catalog().getCode())
                        .thenComparing(match -> match.catalog().getName()))
                .map(match -> resultMapper.toResult(match.catalog()))
                .toList();
    }

    private int rank(DiagnosisCatalog catalog, String keyword) {
        String code = VietnameseTextNormalizer.normalize(catalog.getCode());
        String name = VietnameseTextNormalizer.normalize(catalog.getName());
        String abbreviation = VietnameseTextNormalizer.normalize(catalog.getAbbreviation());
        if (code.equals(keyword)) {
            return 0;
        }
        if (name.equals(keyword)) {
            return 1;
        }
        if (abbreviation != null && abbreviation.equals(keyword)) {
            return 2;
        }
        if (code.startsWith(keyword)) {
            return 3;
        }
        if (name.startsWith(keyword)) {
            return 4;
        }
        if (abbreviation != null && abbreviation.startsWith(keyword)) {
            return 5;
        }
        if (code.contains(keyword)) {
            return 6;
        }
        if (name.contains(keyword)) {
            return 7;
        }
        if (abbreviation != null && abbreviation.contains(keyword)) {
            return 8;
        }
        return -1;
    }

    private record RankedMatch(DiagnosisCatalog catalog, int rank) {
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
