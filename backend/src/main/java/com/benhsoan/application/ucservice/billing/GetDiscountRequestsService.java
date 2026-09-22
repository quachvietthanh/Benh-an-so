package com.benhsoan.application.ucservice.billing;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.billing.exception.DiscountRequestNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.DiscountRequestResult;
import com.benhsoan.port.inbound.billing.GetDiscountRequestsUseCase;
import com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository;
import com.benhsoan.port.outbound.repository.billing.DiscountRequestSearchCriteria;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetDiscountRequestsService implements GetDiscountRequestsUseCase {

    private final DiscountRequestRepository discountRequestRepository;
    private final DiscountRequestResultMapper resultMapper;

    @Override
    public DiscountRequestResult getById(UUID id) {
        if (id == null) {
            throw new ValidationException("Mã đề nghị giảm giá là bắt buộc.");
        }

        return discountRequestRepository.findById(id)
                .map(resultMapper::toResult)
                .orElseThrow(() -> new DiscountRequestNotFoundException(id));
    }

    @Override
    public List<DiscountRequestResult> getByVisitId(UUID visitId) {
        if (visitId == null) {
            throw new ValidationException("Mã lượt khám (visitId) là bắt buộc.");
        }

        return discountRequestRepository.findByVisitId(visitId)
                .stream()
                .map(resultMapper::toResult)
                .toList();
    }

    @Override
    public Page<DiscountRequestResult> search(DiscountRequestSearchCriteria criteria, Pageable pageable) {
        return discountRequestRepository.search(criteria, pageable)
                .map(resultMapper::toResult);
    }
}
