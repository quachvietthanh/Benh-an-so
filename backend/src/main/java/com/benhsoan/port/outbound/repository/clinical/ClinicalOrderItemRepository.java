package com.benhsoan.port.outbound.repository.clinical;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.clinical.ClinicalOrderItem;
public interface ClinicalOrderItemRepository {

    Optional<ClinicalOrderItem> findById(UUID id);

    ClinicalOrderItem save(ClinicalOrderItem item);

    List<ClinicalOrderItem> findByClinicalOrderIdIn(Collection<UUID> clinicalOrderIds);

    List<ClinicalOrderItem> saveAll(Collection<ClinicalOrderItem> items);

    boolean existsByClinicalOrderIdAndClinicalServiceId(UUID clinicalOrderId, UUID clinicalServiceId);
}
