package com.benhsoan.adapter.inbound.rest.request.billing;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.PaymentMethod;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecordPaymentRequest(
        @NotNull UUID visitId,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal examFee,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal medicineFee,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal amountPaid,
        PaymentMethod paymentMethod,
        @Size(max = 100, message = "Mã tham chiếu giao dịch không được vượt quá 100 ký tự.")
        String referenceNumber,
        List<@Valid PaymentMethodItemRequest> paymentMethods
) {
    public RecordPaymentRequest(
            UUID visitId,
            BigDecimal examFee,
            BigDecimal medicineFee,
            BigDecimal amountPaid,
            PaymentMethod paymentMethod
    ) {
        this(visitId, examFee, medicineFee, amountPaid, paymentMethod, null, null);
    }

    public RecordPaymentRequest(
            UUID visitId,
            BigDecimal examFee,
            BigDecimal medicineFee,
            BigDecimal amountPaid,
            PaymentMethod paymentMethod,
            List<PaymentMethodItemRequest> paymentMethods
    ) {
        this(visitId, examFee, medicineFee, amountPaid, paymentMethod, null, paymentMethods);
    }
}
