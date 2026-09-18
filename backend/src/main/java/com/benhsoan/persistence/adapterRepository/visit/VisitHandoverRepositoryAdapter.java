package com.benhsoan.persistence.adapterRepository.visit;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.visit.VisitHandover;
import com.benhsoan.persistence.entity.visit.VisitHandoverEntity;
import com.benhsoan.persistence.jpaRepository.visit.JpaVisitHandoverRepository;
import com.benhsoan.persistence.mapper.visit.VisitHandoverPersistenceMapper;
import com.benhsoan.port.outbound.repository.visit.VisitHandoverRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class VisitHandoverRepositoryAdapter implements VisitHandoverRepository {

    private final JpaVisitHandoverRepository jpaRepository;
    private final VisitHandoverPersistenceMapper mapper;

    @Override
    public VisitHandover save(VisitHandover handover) {
        VisitHandoverEntity entity = mapper.toEntity(handover);
        VisitHandoverEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<VisitHandover> findByVisitId(UUID visitId) {
        return jpaRepository.findByVisitIdOrderByHandedOverAtAsc(visitId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
