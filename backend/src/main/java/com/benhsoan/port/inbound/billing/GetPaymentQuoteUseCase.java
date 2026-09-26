package com.benhsoan.port.inbound.billing;

import com.benhsoan.port.dto.command.billing.GetPaymentQuoteCommand;
import com.benhsoan.port.dto.result.PaymentQuoteResult;

public interface GetPaymentQuoteUseCase {

    PaymentQuoteResult quote(GetPaymentQuoteCommand command);
}
