package com.benhsoan.port.outbound.repository.crudRepository.clinical;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.clinical.ClinicalOrderItem;
import com.benhsoan.port.outbound.repository.BaseRepository;

public interface ClinicalOrderItemRepository extends BaseRepository<ClinicalOrderItem, UUID> {

    List<ClinicalOrderItem> findByClinicalOrderIdIn(Collection<UUID> clinicalOrderIds);

    List<ClinicalOrderItem> saveAll(Collection<ClinicalOrderItem> items);

    boolean existsByClinicalOrderIdAndClinicalServiceId(UUID clinicalOrderId, UUID clinicalServiceId);
}
