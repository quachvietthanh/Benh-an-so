package com.benhsoan.port.outbound.repository.billing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.billing.DiscountRequest;
import com.benhsoan.domain.billing.enums.DiscountRequestStatus;

public interface DiscountRequestRepository {

    DiscountRequest save(DiscountRequest discountRequest);

    Optional<DiscountRequest> findById(UUID id);

    Optional<DiscountRequest> findByIdForUpdate(UUID id);

    Optional<DiscountRequest> findByVisitIdAndStatus(UUID visitId, DiscountRequestStatus status);

    List<DiscountRequest> findByVisitId(UUID visitId);

    boolean existsByVisitIdAndStatus(UUID visitId, DiscountRequestStatus status);

    Page<DiscountRequest> search(DiscountRequestSearchCriteria criteria, Pageable pageable);
}
