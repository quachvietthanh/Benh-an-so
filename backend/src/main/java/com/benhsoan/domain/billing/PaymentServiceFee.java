package com.benhsoan.domain.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentServiceFee {

    private UUID id;
    private UUID paymentId;
    private UUID clinicalOrderItemId;
    private String serviceName;
    private BigDecimal amount;
    private Instant createdAt;

    private PaymentServiceFee(
            UUID id,
            UUID paymentId,
            UUID clinicalOrderItemId,
            String serviceName,
            BigDecimal amount,
            Instant createdAt
    ) {
        this.id = require(id, "Payment service fee id is required.");
        this.paymentId = require(paymentId, "Payment id is required.");
        this.clinicalOrderItemId = require(clinicalOrderItemId, "Clinical order item id is required.");
        this.serviceName = requireText(serviceName, "Service name is required.");
        this.amount = requireNonNegative(amount, "Service fee amount is required.");
        this.createdAt = require(createdAt, "Service fee creation time is required.");
    }

    public static PaymentServiceFee create(
            UUID id,
            UUID paymentId,
            UUID clinicalOrderItemId,
            String serviceName,
            BigDecimal amount,
            Instant createdAt
    ) {
        return new PaymentServiceFee(id, paymentId, clinicalOrderItemId, serviceName, amount, createdAt);
    }

    public static PaymentServiceFee restore(
            UUID id,
            UUID paymentId,
            UUID clinicalOrderItemId,
            String serviceName,
            BigDecimal amount,
            Instant createdAt
    ) {
        return new PaymentServiceFee(id, paymentId, clinicalOrderItemId, serviceName, amount, createdAt);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
        return value.trim();
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(message);
        }
        return value;
    }

    private static <T> T require(T value, String message) {
        if (Objects.isNull(value)) {
            throw new ValidationException(message);
        }
        return value;
    }
}
