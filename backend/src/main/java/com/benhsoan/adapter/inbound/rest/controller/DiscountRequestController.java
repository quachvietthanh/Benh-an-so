package com.benhsoan.adapter.inbound.rest.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.BillingRestMapper;
import com.benhsoan.adapter.inbound.rest.request.billing.CreateDiscountRequest;
import com.benhsoan.adapter.inbound.rest.request.billing.RejectDiscountRequest;
import com.benhsoan.adapter.inbound.rest.response.billing.DiscountRequestResponse;
import com.benhsoan.domain.billing.enums.DiscountRequestStatus;
import com.benhsoan.domain.billing.enums.DiscountType;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.inbound.billing.ApproveDiscountRequestUseCase;
import com.benhsoan.port.inbound.billing.CreateDiscountRequestUseCase;
import com.benhsoan.port.inbound.billing.GetDiscountRequestsUseCase;
import com.benhsoan.port.inbound.billing.RejectDiscountRequestUseCase;
import com.benhsoan.port.outbound.repository.billing.DiscountRequestSearchCriteria;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/invoices/discount-requests")
@RequiredArgsConstructor
@Validated
public class DiscountRequestController {

    private final CreateDiscountRequestUseCase createDiscountRequestUseCase;
    private final ApproveDiscountRequestUseCase approveDiscountRequestUseCase;
    private final RejectDiscountRequestUseCase rejectDiscountRequestUseCase;
    private final GetDiscountRequestsUseCase getDiscountRequestsUseCase;
    private final BillingRestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission("INVOICE_CREATE")
    public DiscountRequestResponse create(@Valid @RequestBody CreateDiscountRequest request) {
        return mapper.toResponse(createDiscountRequestUseCase.create(mapper.toCommand(request)));
    }

    @GetMapping
    @RequirePermission("INVOICE_READ")
    public Page<DiscountRequestResponse> search(
            @RequestParam(required = false) UUID visitId,
            @RequestParam(required = false) DiscountRequestStatus status,
            @RequestParam(required = false) DiscountType discountType,
            @RequestParam(required = false) UUID requestedBy,
            @RequestParam(required = false) UUID approvedBy,
            @RequestParam(required = false) Instant requestedFrom,
            @RequestParam(required = false) Instant requestedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validatePage(page, size);
        if (requestedFrom != null && requestedTo != null && requestedFrom.isAfter(requestedTo)) {
            throw new ValidationException("Thời gian bắt đầu tìm kiếm phải trước hoặc bằng thời gian kết thúc.");
        }

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "requestedAt")
        );

        return mapper.toDiscountResponse(getDiscountRequestsUseCase.search(
                new DiscountRequestSearchCriteria(
                        visitId,
                        status,
                        discountType,
                        requestedBy,
                        approvedBy,
                        requestedFrom,
                        requestedTo
                ),
                pageable
        ));
    }

    @GetMapping("/{id}")
    @RequirePermission("INVOICE_READ")
    public DiscountRequestResponse getById(@PathVariable UUID id) {
        return mapper.toResponse(getDiscountRequestsUseCase.getById(id));
    }

    @PostMapping("/{id}/approve")
    @RequirePermission("INVOICE_UPDATE")
    public DiscountRequestResponse approve(@PathVariable UUID id) {
        return mapper.toResponse(approveDiscountRequestUseCase.approve(id));
    }

    @PostMapping("/{id}/reject")
    @RequirePermission("INVOICE_UPDATE")
    public DiscountRequestResponse reject(
            @PathVariable UUID id,
            @Valid @RequestBody RejectDiscountRequest request
    ) {
        return mapper.toResponse(rejectDiscountRequestUseCase.reject(mapper.toCommand(id, request)));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ValidationException("Chỉ số trang không được âm và kích thước trang phải từ 1 đến 100.");
        }
    }
}
