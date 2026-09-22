package com.benhsoan.port.inbound.billing;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.port.dto.result.DiscountRequestResult;
import com.benhsoan.port.outbound.repository.billing.DiscountRequestSearchCriteria;

public interface GetDiscountRequestsUseCase {

    DiscountRequestResult getById(UUID id);

    List<DiscountRequestResult> getByVisitId(UUID visitId);

    Page<DiscountRequestResult> search(DiscountRequestSearchCriteria criteria, Pageable pageable);
}
