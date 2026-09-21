package com.benhsoan.port.inbound.billing;

import com.benhsoan.port.dto.command.billing.CloseCashierShiftCommand;
import com.benhsoan.port.dto.result.CashierShiftResult;

public interface CloseCashierShiftUseCase {

    CashierShiftResult close(CloseCashierShiftCommand command);
}
