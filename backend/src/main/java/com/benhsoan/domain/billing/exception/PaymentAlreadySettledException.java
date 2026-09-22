package com.benhsoan.domain.billing.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class PaymentAlreadySettledException extends BillingException {

    public PaymentAlreadySettledException(UUID paymentId) {
        super(
                DomainErrorCode.PAYMENT_ALREADY_SETTLED,
                "Khoản thu (" + paymentId + ") đã được chốt ca thu ngân, không thể hoàn tiền trực tiếp; "
                        + "vui lòng thực hiện thông qua hóa đơn điều chỉnh theo quy tắc QTN-09 và QTN-38."
        );
    }
}
