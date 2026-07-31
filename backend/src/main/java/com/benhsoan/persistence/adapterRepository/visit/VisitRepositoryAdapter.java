package com.benhsoan.persistence.adapterRepository.visit;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
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

    @Override
    public Optional<Visit> findTopByOrderByVisitCodeDesc() {
        return jpaRepository.findTopByOrderByVisitCodeDesc().map(mapper::toDomain);
    }

    @Override
    public List<Visit> findByPatientIdOrderByVisitAtDesc(UUID patientId) {
        return jpaRepository.findByPatientIdOrderByVisitAtDesc(patientId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByPatientIdAndStatusIn(UUID patientId, Collection<VisitStatus> statuses) {
        return jpaRepository.existsByPatientIdAndStatusIn(patientId, statuses);
    }
}
