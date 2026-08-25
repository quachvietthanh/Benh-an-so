package com.benhsoan.application.ucservice.medicalrecord;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogNotFoundException;
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
        return repository.search(query, true)
                .stream()
                .map(resultMapper::toResult)
                .toList();
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
