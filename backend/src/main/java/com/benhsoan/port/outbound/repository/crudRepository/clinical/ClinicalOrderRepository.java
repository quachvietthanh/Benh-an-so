package com.benhsoan.port.outbound.repository.crudRepository.clinical;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.clinical.ClinicalOrder;
import com.benhsoan.port.outbound.repository.BaseRepository;

public interface ClinicalOrderRepository extends BaseRepository<ClinicalOrder, UUID> {

    Page<ClinicalOrder> findByVisitId(UUID visitId, Pageable pageable);

    boolean existsByOrderCode(String orderCode);
}
