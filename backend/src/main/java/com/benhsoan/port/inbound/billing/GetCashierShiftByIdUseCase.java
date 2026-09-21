package com.benhsoan.port.inbound.billing;

import java.util.UUID;

import com.benhsoan.port.dto.result.CashierShiftResult;

public interface GetCashierShiftByIdUseCase {

    CashierShiftResult getById(UUID id);
}
