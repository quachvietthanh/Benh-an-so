package com.benhsoan.port.inbound.billing;

import com.benhsoan.port.dto.command.billing.ConfirmCashierShiftCommand;
import com.benhsoan.port.dto.result.CashierShiftResult;

public interface ConfirmCashierShiftUseCase {

    CashierShiftResult confirm(ConfirmCashierShiftCommand command);
}
