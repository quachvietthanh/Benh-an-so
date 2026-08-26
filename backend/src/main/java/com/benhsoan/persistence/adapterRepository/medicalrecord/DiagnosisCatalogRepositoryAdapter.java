package com.benhsoan.persistence.adapterRepository.medicalrecord;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaDiagnosisCatalogRepository;
import com.benhsoan.persistence.mapper.medicalrecord.DiagnosisCatalogPersistenceMapper;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DiagnosisCatalogRepositoryAdapter implements DiagnosisCatalogRepository {

    private final JpaDiagnosisCatalogRepository jpaRepository;
    private final DiagnosisCatalogPersistenceMapper mapper;

    @Override
    public DiagnosisCatalog save(DiagnosisCatalog diagnosisCatalog) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(diagnosisCatalog)));
    }

    @Override
    public Optional<DiagnosisCatalog> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(normalizeCode(code));
    }

    @Override
    public List<DiagnosisCatalog> search(String keyword, Boolean active) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return jpaRepository.search(normalizedKeyword, active).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<DiagnosisCatalog> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code, String name) {
        return jpaRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(code, name)
                .stream().map(mapper::toDomain).toList();
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }
}
