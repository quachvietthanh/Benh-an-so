package com.benhsoan.persistence.adapterRepository.clinical;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.clinical.ClinicalOrderItem;
import com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus;
import com.benhsoan.persistence.entity.clinical.ClinicalOrderItemEntity;
import com.benhsoan.persistence.jpaRepository.clinical.JpaClinicalOrderItemRepository;
import com.benhsoan.persistence.mapper.clinical.ClinicalOrderItemPersistenceMapper;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderItemRepository;
import com.benhsoan.port.outbound.repository.clinical.BillableClinicalService;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ClinicalOrderItemRepositoryAdapter implements ClinicalOrderItemRepository {

    private final JpaClinicalOrderItemRepository jpaRepository;
    private final ClinicalOrderItemPersistenceMapper mapper;

    @Override
    public Optional<ClinicalOrderItem> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public ClinicalOrderItem save(ClinicalOrderItem item) {
        ClinicalOrderItemEntity savedEntity = jpaRepository.save(mapper.toEntity(item));
        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<ClinicalOrderItem> findByClinicalOrderIdIn(Collection<UUID> clinicalOrderIds) {
        if (clinicalOrderIds == null || clinicalOrderIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByClinicalOrderIdIn(clinicalOrderIds).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<BillableClinicalService> findBillableByVisitId(UUID visitId) {
        if (visitId == null) {
            return List.of();
        }
        return jpaRepository.findBillableByVisitId(visitId, ClinicalOrderItemStatus.COMPLETED).stream()
                .map(view -> new BillableClinicalService(
                        view.getClinicalOrderItemId(),
                        view.getServiceCatalogId(),
                        view.getServiceName()
                ))
                .toList();
    }

    @Override
    public List<ClinicalOrderItem> saveAll(Collection<ClinicalOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<ClinicalOrderItemEntity> entities = items.stream().map(mapper::toEntity).toList();
        return jpaRepository.saveAll(entities).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByClinicalOrderIdAndClinicalServiceId(UUID clinicalOrderId, UUID clinicalServiceId) {
        return jpaRepository.existsByClinicalOrderIdAndClinicalServiceId(clinicalOrderId, clinicalServiceId);
    }
}
