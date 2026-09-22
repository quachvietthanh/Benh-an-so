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

import com.benhsoan.adapter.inbound.rest.mapper.CashierShiftRestMapper;
import com.benhsoan.adapter.inbound.rest.request.billing.CloseShiftRequest;
import com.benhsoan.adapter.inbound.rest.request.billing.ConfirmShiftRequest;
import com.benhsoan.adapter.inbound.rest.response.billing.CashierShiftResponse;
import com.benhsoan.adapter.inbound.rest.response.billing.CurrentShiftSummaryResponse;
import com.benhsoan.domain.billing.enums.CashierShiftStatus;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.command.billing.SearchCashierShiftsQuery;
import com.benhsoan.port.inbound.billing.CloseCashierShiftUseCase;
import com.benhsoan.port.inbound.billing.ConfirmCashierShiftUseCase;
import com.benhsoan.port.inbound.billing.GetCashierShiftByIdUseCase;
import com.benhsoan.port.inbound.billing.GetCurrentShiftSummaryUseCase;
import com.benhsoan.port.inbound.billing.SearchCashierShiftsUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/cashier-shifts")
@RequiredArgsConstructor
@Validated
public class CashierShiftController {

    private final GetCurrentShiftSummaryUseCase getCurrentShiftSummaryUseCase;
    private final CloseCashierShiftUseCase closeCashierShiftUseCase;
    private final ConfirmCashierShiftUseCase confirmCashierShiftUseCase;
    private final SearchCashierShiftsUseCase searchCashierShiftsUseCase;
    private final GetCashierShiftByIdUseCase getCashierShiftByIdUseCase;
    private final CashierShiftRestMapper mapper;

    @GetMapping("/current-summary")
    @RequirePermission("CASHIER_SHIFT_READ")
    public CurrentShiftSummaryResponse getCurrentSummary() {
        return mapper.toResponse(getCurrentShiftSummaryUseCase.getCurrentSummary());
    }

    @PostMapping("/close")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission("CASHIER_SHIFT_CREATE")
    public CashierShiftResponse close(@Valid @RequestBody CloseShiftRequest request) {
        return mapper.toResponse(closeCashierShiftUseCase.close(mapper.toCommand(request)));
    }

    @PostMapping("/{id}/confirm")
    @RequirePermission("CASHIER_SHIFT_CONFIRM")
    public CashierShiftResponse confirm(
            @PathVariable UUID id,
            @RequestBody(required = false) ConfirmShiftRequest request
    ) {
        return mapper.toResponse(confirmCashierShiftUseCase.confirm(mapper.toCommand(id, request)));
    }

    @GetMapping
    @RequirePermission("CASHIER_SHIFT_READ")
    public Page<CashierShiftResponse> search(
            @RequestParam(required = false) UUID cashierId,
            @RequestParam(required = false) CashierShiftStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validatePage(page, size);
        if (from != null && to != null && from.isAfter(to)) {
            throw new ValidationException("Thời điểm bắt đầu phải trước hoặc bằng thời điểm kết thúc.");
        }

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return searchCashierShiftsUseCase.search(
                new SearchCashierShiftsQuery(cashierId, status, from, to, pageable)
        ).map(mapper::toResponse);
    }

    @GetMapping("/{id}")
    @RequirePermission("CASHIER_SHIFT_READ")
    public CashierShiftResponse getById(@PathVariable UUID id) {
        return mapper.toResponse(getCashierShiftByIdUseCase.getById(id));
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new ValidationException("Page index must not be less than zero.");
        }
        if (size <= 0) {
            throw new ValidationException("Page size must be greater than zero.");
        }
    }
}
