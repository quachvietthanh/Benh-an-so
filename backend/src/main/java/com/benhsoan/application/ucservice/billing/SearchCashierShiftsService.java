package com.benhsoan.application.ucservice.billing;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.command.billing.SearchCashierShiftsQuery;
import com.benhsoan.port.dto.result.CashierShiftResult;
import com.benhsoan.port.inbound.billing.SearchCashierShiftsUseCase;
import com.benhsoan.port.outbound.repository.billing.CashierShiftRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchCashierShiftsService implements SearchCashierShiftsUseCase {

    private final CashierShiftRepository cashierShiftRepository;
    private final CashierShiftResultMapper resultMapper;

    @Override
    public Page<CashierShiftResult> search(SearchCashierShiftsQuery query) {
        if (query == null) {
            return Page.empty();
        }

        return cashierShiftRepository.search(
                query.cashierId(),
                query.status(),
                query.from(),
                query.to(),
                query.pageable()
        ).map(resultMapper::toResult);
    }
}
