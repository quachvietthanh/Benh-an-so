package com.benhsoan.adapter.inbound.rest.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
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

import com.benhsoan.adapter.inbound.rest.mapper.ClinicalOrderRestMapper;
import com.benhsoan.adapter.inbound.rest.request.clinical.CancelClinicalOrderRequest;
import com.benhsoan.adapter.inbound.rest.request.clinical.CreateClinicalOrderRequest;
import com.benhsoan.adapter.inbound.rest.response.clinical.ClinicalOrderResponse;
import com.benhsoan.adapter.inbound.rest.response.clinical.PendingClinicalOrderResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.command.clinical.CancelClinicalOrderCommand;
import com.benhsoan.port.dto.command.clinical.CancelClinicalOrderItemCommand;
import com.benhsoan.port.dto.command.clinical.GetClinicalOrdersByVisitQuery;
import com.benhsoan.port.dto.command.clinical.GetPendingClinicalOrdersQuery;
import com.benhsoan.port.inbound.clinical.CancelClinicalOrderUseCase;
import com.benhsoan.port.inbound.clinical.CreateClinicalOrderUseCase;
import com.benhsoan.port.inbound.clinical.GetClinicalOrdersByVisitUseCase;
import com.benhsoan.port.inbound.clinical.GetPendingClinicalOrdersUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/clinical-orders")
@RequiredArgsConstructor
@Validated
public class ClinicalOrderController {

    private final CreateClinicalOrderUseCase createClinicalOrderUseCase;
    private final GetClinicalOrdersByVisitUseCase getClinicalOrdersByVisitUseCase;
    private final GetPendingClinicalOrdersUseCase getPendingClinicalOrdersUseCase;
    private final CancelClinicalOrderUseCase cancelClinicalOrderUseCase;
    private final ClinicalOrderRestMapper mapper;

    @PostMapping("/visits/{visitId}")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission("CLINICAL_ORDER_CREATE")
    public ClinicalOrderResponse create(
            @PathVariable UUID visitId,
            @Valid @RequestBody CreateClinicalOrderRequest request
    ) {
        return mapper.toResponse(createClinicalOrderUseCase.createOrder(visitId, mapper.toCommand(request)));
    }

    @GetMapping("/visits/{visitId}")
    @RequirePermission("CLINICAL_ORDER_READ")
    public Page<ClinicalOrderResponse> getByVisitId(
            @PathVariable UUID visitId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return getClinicalOrdersByVisitUseCase.getOrdersByVisit(new GetClinicalOrdersByVisitQuery(visitId, page, size))
                .map(mapper::toResponse);
    }

    @GetMapping("/pending")
    @RequirePermission("CLINICAL_ORDER_READ")
    public Page<PendingClinicalOrderResponse> getPendingOrders(
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return getPendingClinicalOrdersUseCase.getPendingOrders(
                new GetPendingClinicalOrdersQuery(patientId, doctorId, fromDate, toDate, page, size)
        ).map(mapper::toPendingResponse);
    }

    @PostMapping("/{orderId}/cancel")
    @RequirePermission("CLINICAL_ORDER_CANCEL")
    public ClinicalOrderResponse cancelOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody CancelClinicalOrderRequest request
    ) {
        return mapper.toResponse(cancelClinicalOrderUseCase.cancelOrder(
                new CancelClinicalOrderCommand(orderId, request.cancelReason())
        ));
    }

    @PostMapping("/items/{itemId}/cancel")
    @RequirePermission("CLINICAL_ORDER_CANCEL")
    public ClinicalOrderResponse cancelOrderItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody CancelClinicalOrderRequest request
    ) {
        return mapper.toResponse(cancelClinicalOrderUseCase.cancelOrderItem(
                new CancelClinicalOrderItemCommand(itemId, request.cancelReason())
        ));
    }
}
