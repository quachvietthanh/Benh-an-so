package com.benhsoan.port.outbound.repository.clinical;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.clinical.ClinicalOrderItem;
import com.benhsoan.port.dto.result.PendingClinicalOrderResult;

public interface ClinicalOrderItemRepository {

    Optional<ClinicalOrderItem> findById(UUID id);

    Optional<ClinicalOrderItem> findByIdForUpdate(UUID id);

    ClinicalOrderItem save(ClinicalOrderItem item);

    List<ClinicalOrderItem> findByClinicalOrderIdIn(Collection<UUID> clinicalOrderIds);

    List<ClinicalOrderItem> findByIdIn(Collection<UUID> ids);

    List<BillableClinicalService> findBillableByVisitId(UUID visitId);

    List<BillableClinicalService> findBillableByVisitIdIn(Collection<UUID> visitIds);

    List<ClinicalOrderItem> saveAll(Collection<ClinicalOrderItem> items);

    boolean existsByClinicalOrderIdAndClinicalServiceId(UUID clinicalOrderId, UUID clinicalServiceId);

    long countPendingByVisitId(UUID visitId);

    List<String> findPendingServiceNamesByVisitId(UUID visitId);

    Page<PendingClinicalOrderResult> findPendingOrders(
            UUID patientId,
            UUID doctorId,
            Instant fromDate,
            Instant toDate,
            Instant now,
            Pageable pageable
    );
}
