package com.benhsoan.persistence.adapterRepository.visit;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.visit.Visit;
import com.benhsoan.persistence.jpaRepository.visit.JpaVisitRepository;
import com.benhsoan.persistence.mapper.visit.VisitPersistenceMapper;
import com.benhsoan.port.outbound.repository.crudRepository.visit.VisitRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class VisitRepositoryAdapter implements VisitRepository {

    private final JpaVisitRepository jpaRepository;
    private final VisitPersistenceMapper mapper;

    @Override
    public Optional<Visit> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Visit save(Visit visit) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(visit)));
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public Optional<Visit> findByVisitCode(String visitCode) {
        return jpaRepository.findByVisitCode(visitCode).map(mapper::toDomain);
    }
}
