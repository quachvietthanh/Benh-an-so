package com.benhsoan.port.outbound.repository.clinical;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.clinical.ClinicalOrder;
public interface ClinicalOrderRepository {

    Optional<ClinicalOrder> findById(UUID id);

    ClinicalOrder save(ClinicalOrder order);

    Page<ClinicalOrder> findByVisitId(UUID visitId, Pageable pageable);

    boolean existsByOrderCode(String orderCode);
}
