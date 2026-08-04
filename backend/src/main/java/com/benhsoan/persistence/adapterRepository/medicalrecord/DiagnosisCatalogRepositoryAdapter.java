package com.benhsoan.persistence.adapterRepository.medicalrecord;

import java.util.List;
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
    public Optional<DiagnosisCatalog> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public DiagnosisCatalog save(DiagnosisCatalog diagnosis) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(diagnosis)));
    }

    @Override
    public List<DiagnosisCatalog> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code, String name) {
        return jpaRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(code, name)
                .stream().map(mapper::toDomain).toList();
    }
}
