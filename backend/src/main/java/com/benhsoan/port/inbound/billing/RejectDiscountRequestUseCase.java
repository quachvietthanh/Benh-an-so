package com.benhsoan.port.inbound.billing;

import com.benhsoan.port.dto.command.billing.RejectDiscountRequestCommand;
import com.benhsoan.port.dto.result.DiscountRequestResult;

public interface RejectDiscountRequestUseCase {

    DiscountRequestResult reject(RejectDiscountRequestCommand command);
}
