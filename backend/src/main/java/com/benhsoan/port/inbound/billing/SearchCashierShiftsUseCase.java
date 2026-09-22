package com.benhsoan.port.inbound.billing;

import org.springframework.data.domain.Page;

import com.benhsoan.port.dto.command.billing.SearchCashierShiftsQuery;
import com.benhsoan.port.dto.result.CashierShiftResult;

public interface SearchCashierShiftsUseCase {

    Page<CashierShiftResult> search(SearchCashierShiftsQuery query);
}
