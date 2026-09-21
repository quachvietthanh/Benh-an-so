package com.benhsoan.domain.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentMethodItem {

    private UUID id;

    private UUID paymentId;

    private PaymentMethod paymentMethod;

    private BigDecimal amount;

    private String referenceNumber;

    private Instant createdAt;

    private PaymentMethodItem(
            UUID id,
            UUID paymentId,
            PaymentMethod paymentMethod,
            BigDecimal amount,
            String referenceNumber,
            Instant createdAt
    ) {
        this.id = requireNonNull(id, "Payment method item id is required.");
        this.paymentId = requireNonNull(paymentId, "Payment id is required.");
        this.paymentMethod = validateMethod(paymentMethod);
        this.amount = validateAmount(amount);
        this.referenceNumber = validateReferenceNumber(this.paymentMethod, referenceNumber);
        this.createdAt = requireNonNull(createdAt, "Created time is required.");
    }

    public static PaymentMethodItem create(
            UUID id,
            UUID paymentId,
            PaymentMethod paymentMethod,
            BigDecimal amount,
            String referenceNumber,
            Instant createdAt
    ) {
        return new PaymentMethodItem(
                id,
                paymentId,
                paymentMethod,
                amount,
                referenceNumber,
                createdAt
        );
    }

    public static PaymentMethodItem restore(
            UUID id,
            UUID paymentId,
            PaymentMethod paymentMethod,
            BigDecimal amount,
            String referenceNumber,
            Instant createdAt
    ) {
        return new PaymentMethodItem(
                id,
                paymentId,
                paymentMethod,
                amount,
                referenceNumber,
                createdAt
        );
    }

    private static PaymentMethod validateMethod(PaymentMethod method) {
        if (method == null) {
            throw new ValidationException("Payment method is required.");
        }
        if (method == PaymentMethod.MULTIPLE) {
            throw new ValidationException("Payment method item cannot have MULTIPLE as its method.");
        }
        return method;
    }

    private static BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new ValidationException("Amount is required.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Payment method amount must be greater than zero.");
        }
        return amount;
    }

    private static String validateReferenceNumber(PaymentMethod method, String referenceNumber) {
        String trimmed = referenceNumber == null || referenceNumber.trim().isEmpty() ? null : referenceNumber.trim();
        if (method == PaymentMethod.BANK_TRANSFER) {
            if (trimmed == null) {
                throw new ValidationException("Transaction reference number is required for bank transfer.");
            }
        }
        if (trimmed != null && trimmed.length() > 100) {
            throw new ValidationException("Transaction reference number cannot exceed 100 characters.");
        }
        return trimmed;
    }

    private static <T> T requireNonNull(T value, String message) {
        if (Objects.isNull(value)) {
            throw new ValidationException(message);
        }
        return value;
    }
}
