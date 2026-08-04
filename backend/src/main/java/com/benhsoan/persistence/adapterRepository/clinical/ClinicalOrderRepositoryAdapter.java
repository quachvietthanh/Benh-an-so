package com.benhsoan.persistence.adapterRepository.clinical;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.benhsoan.domain.clinical.ClinicalOrder;
import com.benhsoan.persistence.entity.clinical.ClinicalOrderEntity;
import com.benhsoan.persistence.jpaRepository.clinical.JpaClinicalOrderRepository;
import com.benhsoan.persistence.mapper.clinical.ClinicalOrderPersistenceMapper;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ClinicalOrderRepositoryAdapter implements ClinicalOrderRepository {

    private final JpaClinicalOrderRepository jpaRepository;
    private final ClinicalOrderPersistenceMapper mapper;

    @Override
    public Optional<ClinicalOrder> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public ClinicalOrder save(ClinicalOrder order) {
        ClinicalOrderEntity savedEntity = jpaRepository.save(mapper.toEntity(order));
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Page<ClinicalOrder> findByVisitId(UUID visitId, Pageable pageable) {
        return jpaRepository.findByVisitIdOrderByOrderedAtDesc(visitId, pageable).map(mapper::toDomain);
    }

    @Override
    public boolean existsByOrderCode(String orderCode) {
        return jpaRepository.existsByOrderCode(orderCode);
    }
}
