package com.benhsoan.domain.prescription.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class PrescriptionReturnPaymentNotRefundedException extends PrescriptionException {

    public PrescriptionReturnPaymentNotRefundedException() {
        super(
                DomainErrorCode.MEDICATION_RETURN_PAYMENT_NOT_REFUNDED,
                "Medication cannot be returned because the visit payment has not been refunded. Refund the payment first."
        );
    }
}
