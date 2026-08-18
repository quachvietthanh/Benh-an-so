package com.benhsoan.port.inbound.billing;

import com.benhsoan.port.dto.command.billing.RefundPaymentCommand;
import com.benhsoan.port.dto.result.RefundPaymentResult;

public interface RefundPaymentUseCase {

    RefundPaymentResult refund(RefundPaymentCommand command);
}
