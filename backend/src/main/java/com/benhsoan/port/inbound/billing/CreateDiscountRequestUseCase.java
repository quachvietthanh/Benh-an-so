package com.benhsoan.port.inbound.billing;

import com.benhsoan.port.dto.command.billing.CreateDiscountRequestCommand;
import com.benhsoan.port.dto.result.DiscountRequestResult;

public interface CreateDiscountRequestUseCase {

    DiscountRequestResult create(CreateDiscountRequestCommand command);
}
