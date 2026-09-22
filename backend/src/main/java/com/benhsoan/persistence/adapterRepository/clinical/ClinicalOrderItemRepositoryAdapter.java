package com.benhsoan.persistence.adapterRepository.clinical;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.benhsoan.persistence.jpaRepository.clinical.PendingClinicalOrderItemView;
import com.benhsoan.port.dto.result.PendingClinicalOrderResult;

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
    public Optional<ClinicalOrderItem> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(mapper::toDomain);
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
    public List<BillableClinicalService> findBillableByVisitIdIn(Collection<UUID> visitIds) {
        if (visitIds == null || visitIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findBillableByVisitIdIn(visitIds, ClinicalOrderItemStatus.COMPLETED).stream()
                .map(view -> new BillableClinicalService(
                        view.getVisitId(),
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

    @Override
    public long countPendingByVisitId(UUID visitId) {
        if (visitId == null) {
            return 0;
        }
        return jpaRepository.countByVisitIdAndStatus(visitId, ClinicalOrderItemStatus.PENDING);
    }

    @Override
    public List<String> findPendingServiceNamesByVisitId(UUID visitId) {
        if (visitId == null) {
            return List.of();
        }
        return jpaRepository.findPendingServiceNamesByVisitId(visitId, ClinicalOrderItemStatus.PENDING);
    }

    @Override
    public Page<PendingClinicalOrderResult> findPendingOrders(
            UUID patientId,
            UUID doctorId,
            Instant fromDate,
            Instant toDate,
            Instant now,
            Pageable pageable
    ) {
        return jpaRepository.findPendingItems(
                ClinicalOrderItemStatus.PENDING,
                patientId,
                doctorId,
                fromDate,
                toDate,
                pageable
        ).map(view -> mapToPendingResult(view, now));
    }

    private PendingClinicalOrderResult mapToPendingResult(PendingClinicalOrderItemView view, Instant now) {
        long waitingMinutes = 0;
        if (view.getOrderedAt() != null && now != null) {
            waitingMinutes = Math.max(0, Duration.between(view.getOrderedAt(), now).toMinutes());
        }
        return new PendingClinicalOrderResult(
                view.getOrderItemId(),
                view.getOrderId(),
                view.getOrderCode(),
                view.getVisitId(),
                view.getVisitCode(),
                view.getPatientId(),
                view.getPatientCode(),
                view.getPatientFullName(),
                view.getDoctorId(),
                view.getDoctorFullName(),
                view.getClinicalServiceId(),
                view.getServiceCode(),
                view.getServiceName(),
                view.getServiceType(),
                view.getInstruction(),
                view.getClinicalReason(),
                view.getStatus(),
                view.getOrderedAt(),
                waitingMinutes
        );
    }
}
