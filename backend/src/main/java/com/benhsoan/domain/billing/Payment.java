package com.benhsoan.domain.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.domain.billing.exception.PaymentAmountMismatchException;
import com.benhsoan.domain.billing.exception.PaymentNotAllowedException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.enums.VisitStatus;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    private UUID id;

    private UUID visitId;

    private BigDecimal examFee;

    private BigDecimal medicineFee;

    private BigDecimal totalAmount;

    private BigDecimal amountPaid;

    private PaymentMethod paymentMethod;

    private PaymentStatus status;

    private UUID collectedBy;

    private Instant paidAt;

    private Instant createdAt;

    private Payment(
            UUID id,
            UUID visitId,
            BigDecimal examFee,
            BigDecimal medicineFee,
            BigDecimal totalAmount,
            BigDecimal amountPaid,
            PaymentMethod paymentMethod,
            PaymentStatus status,
            UUID collectedBy,
            Instant paidAt,
            Instant createdAt
    ) {
        this.id = requireNonNull(id, "Payment id is required.");
        this.visitId = requireNonNull(visitId, "Visit id is required.");
        this.examFee = validateNonNegative(examFee, "Exam fee is required.");
        this.medicineFee = validateNonNegative(medicineFee, "Medicine fee is required.");
        this.totalAmount = validateTotalAmount(totalAmount, this.examFee, this.medicineFee);
        this.amountPaid = validateAmountPaid(amountPaid, this.totalAmount);
        this.paymentMethod = requireNonNull(paymentMethod, "Payment method is required.");
        this.status = requireNonNull(status, "Payment status is required.");
        this.collectedBy = requireNonNull(collectedBy, "Collector id is required.");
        this.paidAt = requireNonNull(paidAt, "Payment time is required.");
        this.createdAt = requireNonNull(createdAt, "Payment creation time is required.");
    }

    public static Payment record(
            UUID id,
            UUID visitId,
            BigDecimal examFee,
            BigDecimal medicineFee,
            BigDecimal amountPaid,
            PaymentMethod paymentMethod,
            UUID collectedBy,
            Instant paidAt,
            VisitStatus visitStatus,
            boolean dispensingCompleted
    ) {
        validatePaymentEligibility(visitStatus, dispensingCompleted);
        BigDecimal validatedExamFee = validateNonNegative(examFee, "Exam fee is required.");
        BigDecimal validatedMedicineFee = validateNonNegative(medicineFee, "Medicine fee is required.");
        BigDecimal totalAmount = validatedExamFee.add(validatedMedicineFee);

        return new Payment(
                id,
                visitId,
                validatedExamFee,
                validatedMedicineFee,
                totalAmount,
                amountPaid,
                paymentMethod,
                PaymentStatus.RECORDED,
                collectedBy,
                paidAt,
                paidAt
        );
    }

    public static Payment restore(
            UUID id,
            UUID visitId,
            BigDecimal examFee,
            BigDecimal medicineFee,
            BigDecimal totalAmount,
            BigDecimal amountPaid,
            PaymentMethod paymentMethod,
            PaymentStatus status,
            UUID collectedBy,
            Instant paidAt,
            Instant createdAt
    ) {
        return new Payment(
                id,
                visitId,
                examFee,
                medicineFee,
                totalAmount,
                amountPaid,
                paymentMethod,
                status,
                collectedBy,
                paidAt,
                createdAt
        );
    }

    public boolean isRecorded() {
        return status == PaymentStatus.RECORDED;
    }

    public boolean isRefunded() {
        return status == PaymentStatus.REFUNDED;
    }

    public void refund(String reason, UUID refundedBy) {
        requireText(reason, "Refund reason is required.");
        requireNonNull(refundedBy, "Refunded by user id is required.");

        if (status != PaymentStatus.RECORDED && status != PaymentStatus.SUCCESS) {
            throw new PaymentNotAllowedException(
                    "Only successful or recorded payments can be refunded."
            );
        }

        this.status = PaymentStatus.REFUNDED;
    }

    private static void validatePaymentEligibility(
            VisitStatus visitStatus,
            boolean dispensingCompleted
    ) {
        if (visitStatus != VisitStatus.COMPLETED) {
            throw new PaymentNotAllowedException(
                    "Payment can only be recorded after the visit is completed."
            );
        }
        if (!dispensingCompleted) {
            throw new PaymentNotAllowedException(
                    "Payment cannot be recorded before dispensing is completed."
            );
        }
    }

    private static BigDecimal validateTotalAmount(
            BigDecimal totalAmount,
            BigDecimal examFee,
            BigDecimal medicineFee
    ) {
        BigDecimal validatedTotal = validateNonNegative(totalAmount, "Total amount is required.");
        BigDecimal expectedTotal = examFee.add(medicineFee);
        if (validatedTotal.compareTo(expectedTotal) != 0) {
            throw new ValidationException("Total amount must equal exam fee plus medicine fee.");
        }
        return validatedTotal;
    }

    private static BigDecimal validateAmountPaid(
            BigDecimal amountPaid,
            BigDecimal totalAmount
    ) {
        BigDecimal validatedAmountPaid = validateNonNegative(amountPaid, "Amount paid is required.");
        if (validatedAmountPaid.compareTo(totalAmount) != 0) {
            throw new PaymentAmountMismatchException(totalAmount, validatedAmountPaid);
        }
        return validatedAmountPaid;
    }

    private static BigDecimal validateNonNegative(BigDecimal value, String message) {
        if (value == null) {
            throw new ValidationException(message);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(message.replace("is required", "must not be negative"));
        }
        return value;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
        return value.trim();
    }

    private static <T> T requireNonNull(T value, String message) {
        if (Objects.isNull(value)) {
            throw new ValidationException(message);
        }
        return value;
    }
}
