package com.benhsoan.application.ucservice.billing;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.billing.CashierShift;
import com.benhsoan.domain.billing.exception.CashierShiftNotFoundException;
import com.benhsoan.port.dto.result.CashierShiftResult;
import com.benhsoan.port.inbound.billing.GetCashierShiftByIdUseCase;
import com.benhsoan.port.outbound.repository.billing.CashierShiftRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetCashierShiftByIdService implements GetCashierShiftByIdUseCase {

    private final CashierShiftRepository cashierShiftRepository;
    private final CashierShiftResultMapper resultMapper;

    @Override
    public CashierShiftResult getById(UUID id) {
        CashierShift shift = cashierShiftRepository.findById(id)
                .orElseThrow(() -> new CashierShiftNotFoundException(id));
        return resultMapper.toResult(shift);
    }
}
