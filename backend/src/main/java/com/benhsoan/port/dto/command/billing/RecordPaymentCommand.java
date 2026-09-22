package com.benhsoan.port.dto.command.billing;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.PaymentMethod;

import lombok.Builder;

@Builder
public record RecordPaymentCommand(
        UUID visitId,
        BigDecimal examFee,
        BigDecimal medicineFee,
        BigDecimal amountPaid,
        PaymentMethod paymentMethod,
        String referenceNumber,
        List<PaymentMethodItemCommand> paymentMethods
) {
    public RecordPaymentCommand(
            UUID visitId,
            BigDecimal examFee,
            BigDecimal medicineFee,
            BigDecimal amountPaid,
            PaymentMethod paymentMethod
    ) {
        this(visitId, examFee, medicineFee, amountPaid, paymentMethod, null, null);
    }

    public RecordPaymentCommand(
            UUID visitId,
            BigDecimal examFee,
            BigDecimal medicineFee,
            BigDecimal amountPaid,
            PaymentMethod paymentMethod,
            List<PaymentMethodItemCommand> paymentMethods
    ) {
        this(visitId, examFee, medicineFee, amountPaid, paymentMethod, null, paymentMethods);
    }

    public RecordPaymentCommand(
            UUID visitId,
            BigDecimal examFee,
            BigDecimal medicineFee,
            BigDecimal amountPaid,
            PaymentMethod paymentMethod,
            String referenceNumber
    ) {
        this(visitId, examFee, medicineFee, amountPaid, paymentMethod, referenceNumber, null);
    }
}
