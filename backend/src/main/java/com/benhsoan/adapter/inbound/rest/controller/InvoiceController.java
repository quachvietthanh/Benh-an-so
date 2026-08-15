package com.benhsoan.adapter.inbound.rest.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.benhsoan.adapter.inbound.rest.request.billing.AdjustInvoiceRequest;
import com.benhsoan.adapter.inbound.rest.request.billing.CreateInvoiceRequest;
import com.benhsoan.adapter.inbound.rest.request.billing.RecordPaymentRequest;
import com.benhsoan.adapter.inbound.rest.response.billing.InvoiceResponse;
import com.benhsoan.adapter.inbound.rest.response.billing.PayableEncounterResponse;
import com.benhsoan.adapter.inbound.rest.response.billing.PaymentResponse;
import com.benhsoan.domain.auth.enums.Permission;
import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.infrastructure.security.annotation.CheckPermission;
import com.benhsoan.port.dto.command.billing.SearchInvoicesQuery;
import com.benhsoan.port.inbound.billing.AdjustInvoiceUseCase;
import com.benhsoan.port.inbound.billing.CreateInvoiceUseCase;
import com.benhsoan.port.inbound.billing.GetInvoiceByIdUseCase;
import com.benhsoan.port.inbound.billing.GetPayableEncountersUseCase;
import com.benhsoan.port.inbound.billing.RecordPaymentUseCase;
import com.benhsoan.port.inbound.billing.SearchInvoicesUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
@Validated
public class InvoiceController {

    private final RecordPaymentUseCase recordPaymentUseCase;
    private final CreateInvoiceUseCase createInvoiceUseCase;
    private final AdjustInvoiceUseCase adjustInvoiceUseCase;
    private final GetPayableEncountersUseCase getPayableEncountersUseCase;
    private final SearchInvoicesUseCase searchInvoicesUseCase;
    private final GetInvoiceByIdUseCase getInvoiceByIdUseCase;
    private final BillingRestMapper mapper;

    @PostMapping("/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    @CheckPermission(Permission.INVOICE_CREATE)
    public PaymentResponse recordPayment(@Valid @RequestBody RecordPaymentRequest request) {
        return mapper.toResponse(recordPaymentUseCase.record(mapper.toCommand(request)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    @CheckPermission(Permission.INVOICE_CREATE)
    public InvoiceResponse createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        return mapper.toResponse(createInvoiceUseCase.create(mapper.toCommand(request)));
    }

    @GetMapping("/payable")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')")
    @CheckPermission(Permission.INVOICE_READ)
    public Page<PayableEncounterResponse> getPayableEncounters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validatePage(page, size);

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "completedAt")
        );

        return mapper.toPayableResponse(getPayableEncountersUseCase.get(pageable));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')")
    @CheckPermission(Permission.INVOICE_READ)
    public Page<InvoiceResponse> search(
            @RequestParam(required = false) String invoiceCode,
            @RequestParam(required = false) InvoiceType invoiceType,
            @RequestParam(required = false) UUID visitId,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validatePage(page, size);
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new ValidationException("createdFrom must be before or equal to createdTo.");
        }

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return mapper.toInvoiceResponse(searchInvoicesUseCase.search(
                new SearchInvoicesQuery(
                        invoiceCode,
                        invoiceType,
                        visitId,
                        createdFrom,
                        createdTo,
                        pageable
                )
        ));
    }

    @GetMapping("/{invoiceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')")
    @CheckPermission(Permission.INVOICE_READ)
    public InvoiceResponse getById(@PathVariable UUID invoiceId) {
        return mapper.toResponse(getInvoiceByIdUseCase.getById(invoiceId));
    }

    @PostMapping("/{invoiceId}/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MANAGER')")
    @CheckPermission(Permission.INVOICE_UPDATE)
    public InvoiceResponse adjust(
            @PathVariable UUID invoiceId,
            @Valid @RequestBody AdjustInvoiceRequest request
    ) {
        return mapper.toResponse(adjustInvoiceUseCase.adjust(mapper.toCommand(invoiceId, request)));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ValidationException("Page must be non-negative and size must be between 1 and 100.");
        }
    }
}
