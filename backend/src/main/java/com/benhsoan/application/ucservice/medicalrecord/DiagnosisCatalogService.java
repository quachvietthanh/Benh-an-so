package com.benhsoan.application.ucservice.medicalrecord;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.port.dto.result.DiagnosisCatalogResult;
import com.benhsoan.port.inbound.medicalrecord.GetDiagnosisCatalogUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;

import lombok.RequiredArgsConstructor;

/**
 * Step 5 (Simplicity Check): Keeping service simple - one method that delegates to repository with mapping.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiagnosisCatalogService implements GetDiagnosisCatalogUseCase {

    private final DiagnosisCatalogRepository repository;

    @Override
    public List<DiagnosisCatalogResult> search(String query) {
        if (query == null || query.isBlank()) {
            return repository.findAll()
                    .stream()
                    .map(this::toResult)
                    .toList();
        }
        String trimmed = query.trim();
        return repository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(trimmed, trimmed)
                .stream()
                .map(this::toResult)
                .toList();
    }

    private DiagnosisCatalogResult toResult(DiagnosisCatalog dc) {
        return new DiagnosisCatalogResult(
                dc.getId(), dc.getCode(), dc.getName(),
                dc.getDescription(), dc.isActive(),
                dc.getCreatedAt(), dc.getUpdatedAt()
        );
    }
}
