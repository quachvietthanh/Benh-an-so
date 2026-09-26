package com.benhsoan.port.inbound.billing;

import com.benhsoan.port.dto.command.billing.RecordPaymentCommand;
import com.benhsoan.port.dto.result.PaymentResult;

public interface RecordPaymentUseCase {

    PaymentResult record(RecordPaymentCommand command);
}
