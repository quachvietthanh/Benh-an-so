package com.benhsoan.domain.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.domain.billing.exception.PaymentAmountMismatchException;
import com.benhsoan.domain.billing.exception.PaymentAlreadySettledException;
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

    private BigDecimal serviceFee;

    private BigDecimal discountAmount;

    private UUID discountRequestId;

    private BigDecimal totalAmount;

    private BigDecimal amountPaid;

    private PaymentMethod paymentMethod;

    private PaymentStatus status;

    private UUID collectedBy;

    private Instant paidAt;

    private String refundReason;

    private UUID refundedBy;

    private Instant refundedAt;

    private Instant createdAt;

    private UUID cashierShiftId;

    private List<PaymentMethodItem> paymentMethodItems;

    private Payment(
            UUID id,
            UUID visitId,
            BigDecimal examFee,
            BigDecimal medicineFee,
            BigDecimal serviceFee,
            BigDecimal discountAmount,
            UUID discountRequestId,
            BigDecimal totalAmount,
            BigDecimal amountPaid,
            PaymentMethod paymentMethod,
            PaymentStatus status,
            UUID collectedBy,
            Instant paidAt,
            String refundReason,
            UUID refundedBy,
            Instant refundedAt,
            Instant createdAt,
            UUID cashierShiftId,
            List<PaymentMethodItem> paymentMethodItems) {
        this.id = requireNonNull(id, "Payment id is required.");
        this.visitId = requireNonNull(visitId, "Visit id is required.");
        this.examFee = validateNonNegative(examFee, "Exam fee is required.");
        this.medicineFee = validateNonNegative(medicineFee, "Medicine fee is required.");
        this.serviceFee = validateNonNegative(serviceFee, "Service fee is required.");
        this.discountAmount = discountAmount != null
                ? validateNonNegative(discountAmount, "Discount amount is required.")
                : BigDecimal.ZERO;
        this.discountRequestId = discountRequestId;
        this.totalAmount = validateTotalAmount(
                totalAmount,
                this.examFee,
                this.medicineFee,
                this.serviceFee);
        this.amountPaid = validateAmountPaid(amountPaid, this.totalAmount, this.discountAmount);
        this.paymentMethod = requireNonNull(paymentMethod, "Payment method is required.");
        this.status = requireNonNull(status, "Payment status is required.");
        this.collectedBy = requireNonNull(collectedBy, "Collector id is required.");
        this.paidAt = requireNonNull(paidAt, "Payment time is required.");
        this.refundReason = refundReason;
        this.refundedBy = refundedBy;
        this.refundedAt = refundedAt;
        this.createdAt = requireNonNull(createdAt, "Payment creation time is required.");
        this.cashierShiftId = cashierShiftId;
        this.paymentMethodItems = validatePaymentMethodItems(
                paymentMethodItems,
                this.amountPaid,
                this.paymentMethod,
                this.id,
                this.paidAt);
    }

    public static Payment record(
            UUID id,
            UUID visitId,
            BigDecimal examFee,
            BigDecimal medicineFee,
            BigDecimal serviceFee,
            BigDecimal discountAmount,
            UUID discountRequestId,
            BigDecimal amountPaid,
            List<PaymentMethodItem> paymentMethodItems,
            UUID collectedBy,
            Instant paidAt,
            VisitStatus visitStatus,
            boolean dispensingCompleted) {
        validatePaymentEligibility(visitStatus, dispensingCompleted);
        BigDecimal validatedExamFee = validateNonNegative(examFee, "Exam fee is required.");
        BigDecimal validatedMedicineFee = validateNonNegative(medicineFee, "Medicine fee is required.");
        BigDecimal validatedServiceFee = validateNonNegative(serviceFee, "Service fee is required.");
        BigDecimal validatedDiscount = discountAmount != null
                ? validateNonNegative(discountAmount, "Discount amount is required.")
                : BigDecimal.ZERO;
        BigDecimal totalAmount = validatedExamFee.add(validatedMedicineFee).add(validatedServiceFee);
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Payment total amount must be greater than zero.");
        }

        BigDecimal validatedAmountPaid = validateAmountPaid(amountPaid, totalAmount, validatedDiscount);

        if (validatedAmountPaid.compareTo(BigDecimal.ZERO) > 0) {
            if (paymentMethodItems == null || paymentMethodItems.isEmpty()) {
                throw new ValidationException("At least one payment method item is required.");
            }

            BigDecimal sumItems = paymentMethodItems.stream()
                    .map(PaymentMethodItem::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (sumItems.compareTo(validatedAmountPaid) != 0) {
                throw new PaymentAmountMismatchException(validatedAmountPaid, sumItems);
            }
        } else {
            if (paymentMethodItems != null && !paymentMethodItems.isEmpty()) {
                BigDecimal sumItems = paymentMethodItems.stream()
                        .map(PaymentMethodItem::getAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (sumItems.compareTo(BigDecimal.ZERO) != 0) {
                    throw new PaymentAmountMismatchException(BigDecimal.ZERO, sumItems);
                }
            }
        }

        PaymentMethod resolvedMethod = resolvePaymentMethod(paymentMethodItems);

        return new Payment(
                id,
                visitId,
                validatedExamFee,
                validatedMedicineFee,
                validatedServiceFee,
                validatedDiscount,
                discountRequestId,
                totalAmount,
                validatedAmountPaid,
                resolvedMethod,
                PaymentStatus.RECORDED,
                collectedBy,
                paidAt,
                null,
                null,
                null,
                paidAt,
                null,
                paymentMethodItems);
    }

    public static Payment record(
            UUID id,
            UUID visitId,
            BigDecimal examFee,
            BigDecimal medicineFee,
            BigDecimal serviceFee,
            BigDecimal discountAmount,
            UUID discountRequestId,
            BigDecimal amountPaid,
            PaymentMethod paymentMethod,
            UUID collectedBy,
            Instant paidAt,
            VisitStatus visitStatus,
            boolean dispensingCompleted) {
        List<PaymentMethodItem> singleItem = (amountPaid != null && amountPaid.compareTo(BigDecimal.ZERO) == 0)
                ? List.of()
                : List.of(PaymentMethodItem.create(
                        UUID.randomUUID(),
                        id,
                        paymentMethod,
                        amountPaid,
                        null,
                        paidAt));
        return record(
                id,
                visitId,
                examFee,
                medicineFee,
                serviceFee,
                discountAmount,
                discountRequestId,
                amountPaid,
                singleItem,
                collectedBy,
                paidAt,
                visitStatus,
                dispensingCompleted);
    }

    public static Payment record(
            UUID id,
            UUID visitId,
            BigDecimal examFee,
            BigDecimal medicineFee,
            BigDecimal serviceFee,
            BigDecimal amountPaid,
            List<PaymentMethodItem> paymentMethodItems,
            UUID collectedBy,
            Instant paidAt,
            VisitStatus visitStatus,
            boolean dispensingCompleted) {
        return record(
                id,
                visitId,
                examFee,
                medicineFee,
                serviceFee,
                BigDecimal.ZERO,
                null,
                amountPaid,
                paymentMethodItems,
                collectedBy,
                paidAt,
                visitStatus,
                dispensingCompleted);
    }

    public static Payment record(
            UUID id,
            UUID visitId,
            BigDecimal examFee,
            BigDecimal medicineFee,
            BigDecimal serviceFee,
            BigDecimal amountPaid,
            PaymentMethod paymentMethod,
            UUID collectedBy,
            Instant paidAt,
            VisitStatus visitStatus,
            boolean dispensingCompleted) {
        return record(
                id,
                visitId,
                examFee,
                medicineFee,
                serviceFee,
                BigDecimal.ZERO,
                null,
                amountPaid,
                paymentMethod,
                collectedBy,
                paidAt,
                visitStatus,
                dispensingCompleted);
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
            boolean dispensingCompleted) {
        return record(
                id,
                visitId,
                examFee,
                medicineFee,
                BigDecimal.ZERO,
                amountPaid,
                paymentMethod,
                collectedBy,
                paidAt,
                visitStatus,
                dispensingCompleted);
    }

    public static Payment restore(
            UUID id,
            UUID visitId,
            BigDecimal examFee,
            BigDecimal medicineFee,
            BigDecimal serviceFee,
            BigDecimal discountAmount,
            UUID discountRequestId,
            BigDecimal totalAmount,
            BigDecimal amountPaid,
            PaymentMethod paymentMethod,
            PaymentStatus status,
            UUID collectedBy,
            Instant paidAt,
            String refundReason,
            UUID refundedBy,
            Instant refundedAt,
            Instant createdAt,
            UUID cashierShiftId,
            List<PaymentMethodItem> paymentMethodItems) {
        return new Payment(
                id,
                visitId,
                examFee,
                medicineFee,
                serviceFee,
                discountAmount,
                discountRequestId,
                totalAmount,
                amountPaid,
                paymentMethod,
                status,
                collectedBy,
                paidAt,
                refundReason,
                refundedBy,
                refundedAt,
                createdAt,
                cashierShiftId,
                paymentMethodItems);
    }

    public static Payment restore(
            UUID id,
            UUID visitId,
            BigDecimal examFee,
            BigDecimal medicineFee,
            BigDecimal serviceFee,
            BigDecimal discountAmount,
            UUID discountRequestId,
            BigDecimal totalAmount,
            BigDecimal amountPaid,
            PaymentMethod paymentMethod,
            PaymentStatus status,
            UUID collectedBy,
            Instant paidAt,
            String refundReason,
            UUID refundedBy,
            Instant refundedAt,
            Instant createdAt) {
        return restore(
                id,
                visitId,
                examFee,
                medicineFee,
                serviceFee,
                discountAmount,
                discountRequestId,
                totalAmount,
                amountPaid,
                paymentMethod,
                status,
                collectedBy,
                paidAt,
                refundReason,
                refundedBy,
                refundedAt,
                createdAt,
                null,
                null);
    }

    public static Payment restore(
            UUID id,
            UUID visitId,
            BigDecimal examFee,
            BigDecimal medicineFee,
            BigDecimal serviceFee,
            BigDecimal totalAmount,
            BigDecimal amountPaid,
            PaymentMethod paymentMethod,
            PaymentStatus status,
            UUID collectedBy,
            Instant paidAt,
            String refundReason,
            UUID refundedBy,
            Instant refundedAt,
            Instant createdAt,
            UUID cashierShiftId,
            List<PaymentMethodItem> paymentMethodItems) {
        return restore(
                id,
                visitId,
                examFee,
                medicineFee,
                serviceFee,
                BigDecimal.ZERO,
                null,
                totalAmount,
                amountPaid,
                paymentMethod,
                status,
                collectedBy,
                paidAt,
                refundReason,
                refundedBy,
                refundedAt,
                createdAt,
                cashierShiftId,
                paymentMethodItems);
    }

    public static Payment restore(
            UUID id,
            UUID visitId,
            BigDecimal examFee,
            BigDecimal medicineFee,
            BigDecimal serviceFee,
            BigDecimal totalAmount,
            BigDecimal amountPaid,
            PaymentMethod paymentMethod,
            PaymentStatus status,
            UUID collectedBy,
            Instant paidAt,
            String refundReason,
            UUID refundedBy,
            Instant refundedAt,
            Instant createdAt,
            UUID cashierShiftId) {
        return restore(
                id,
                visitId,
                examFee,
                medicineFee,
                serviceFee,
                BigDecimal.ZERO,
                null,
                totalAmount,
                amountPaid,
                paymentMethod,
                status,
                collectedBy,
                paidAt,
                refundReason,
                refundedBy,
                refundedAt,
                createdAt,
                cashierShiftId,
                null);
    }

    public static Payment restore(
            UUID id,
            UUID visitId,
            BigDecimal examFee,
            BigDecimal medicineFee,
            BigDecimal serviceFee,
            BigDecimal totalAmount,
            BigDecimal amountPaid,
            PaymentMethod paymentMethod,
            PaymentStatus status,
            UUID collectedBy,
            Instant paidAt,
            String refundReason,
            UUID refundedBy,
            Instant refundedAt,
            Instant createdAt,
            List<PaymentMethodItem> paymentMethodItems
    ) {
        return restore(
                id,
                visitId,
                examFee,
                medicineFee,
                serviceFee,
                totalAmount,
                amountPaid,
                paymentMethod,
                status,
                collectedBy,
                paidAt,
                refundReason,
                refundedBy,
                refundedAt,
                createdAt,
                null,
                paymentMethodItems
        );
    }

    public static Payment restore(
            UUID id,
            UUID visitId,
            BigDecimal examFee,
            BigDecimal medicineFee,
            BigDecimal serviceFee,
            BigDecimal totalAmount,
            BigDecimal amountPaid,
            PaymentMethod paymentMethod,
            PaymentStatus status,
            UUID collectedBy,
            Instant paidAt,
            String refundReason,
            UUID refundedBy,
            Instant refundedAt,
            Instant createdAt) {
        return restore(
                id,
                visitId,
                examFee,
                medicineFee,
                serviceFee,
                totalAmount,
                amountPaid,
                paymentMethod,
                status,
                collectedBy,
                paidAt,
                refundReason,
                refundedBy,
                refundedAt,
                createdAt,
                (UUID) null,
                null);
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
            String refundReason,
            UUID refundedBy,
            Instant refundedAt,
            Instant createdAt) {
        return restore(
                id,
                visitId,
                examFee,
                medicineFee,
                BigDecimal.ZERO,
                totalAmount,
                amountPaid,
                paymentMethod,
                status,
                collectedBy,
                paidAt,
                refundReason,
                refundedBy,
                refundedAt,
                createdAt);
    }

    public static Payment restore(
            UUID id,
            UUID visitId,
            BigDecimal examFee,
            BigDecimal medicineFee,
            BigDecimal serviceFee,
            BigDecimal totalAmount,
            BigDecimal amountPaid,
            PaymentMethod paymentMethod,
            PaymentStatus status,
            UUID collectedBy,
            Instant paidAt,
            Instant createdAt) {
        return restore(
                id,
                visitId,
                examFee,
                medicineFee,
                serviceFee,
                totalAmount,
                amountPaid,
                paymentMethod,
                status,
                collectedBy,
                paidAt,
                null,
                null,
                null,
                createdAt);
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
            Instant createdAt) {
        return restore(
                id,
                visitId,
                examFee,
                medicineFee,
                BigDecimal.ZERO,
                totalAmount,
                amountPaid,
                paymentMethod,
                status,
                collectedBy,
                paidAt,
                createdAt);
    }

    public boolean isRecorded() {
        return status == PaymentStatus.RECORDED;
    }

    public boolean isRefunded() {
        return status == PaymentStatus.REFUNDED;
    }

    public boolean isSettled() {
        return cashierShiftId != null;
    }

    public void assignToShift(UUID shiftId) {
        if (this.cashierShiftId != null && !this.cashierShiftId.equals(shiftId)) {
            throw new ValidationException("Khoản thu đã được gán cho một ca chốt khác.");
        }
        this.cashierShiftId = requireNonNull(shiftId, "Mã ca chốt không được để trống.");
    }

    public void refund(String reason, UUID refundedBy, Instant refundedAt) {
        if (isSettled()) {
            throw new PaymentAlreadySettledException(this.id);
        }

        String validatedReason = requireText(reason, "Refund reason is required.");
        UUID validatedRefundedBy = requireNonNull(
                refundedBy,
                "Refunded by user id is required.");
        Instant validatedRefundedAt = requireNonNull(
                refundedAt,
                "Refund time is required.");

        if (status != PaymentStatus.RECORDED && status != PaymentStatus.SUCCESS) {
            throw new PaymentNotAllowedException(
                    "Only successful or recorded payments can be refunded.");
        }

        this.status = PaymentStatus.REFUNDED;
        this.refundReason = validatedReason;
        this.refundedBy = validatedRefundedBy;
        this.refundedAt = validatedRefundedAt;
    }

    private static void validatePaymentEligibility(
            VisitStatus visitStatus,
            boolean dispensingCompleted) {
        if (visitStatus == VisitStatus.CANCELLED) {
            throw new PaymentNotAllowedException(
                    "Payment cannot be recorded for cancelled visits.");
        }
        if (!dispensingCompleted) {
            throw new PaymentNotAllowedException(
                    "Payment cannot be recorded before dispensing is completed.");
        }
    }

    private static BigDecimal validateTotalAmount(
            BigDecimal totalAmount,
            BigDecimal examFee,
            BigDecimal medicineFee,
            BigDecimal serviceFee) {
        BigDecimal validatedTotal = validateNonNegative(totalAmount, "Total amount is required.");
        BigDecimal expectedTotal = examFee.add(medicineFee).add(serviceFee);
        if (validatedTotal.compareTo(expectedTotal) != 0) {
            throw new ValidationException(
                    "Total amount must equal exam fee plus medicine fee plus service fee.");
        }
        return validatedTotal;
    }

    private static BigDecimal validateAmountPaid(
            BigDecimal amountPaid,
            BigDecimal totalAmount,
            BigDecimal discountAmount) {
        BigDecimal validatedAmountPaid = validateNonNegative(amountPaid, "Amount paid is required.");
        BigDecimal expectedAmountPaid = totalAmount.subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO);
        if (expectedAmountPaid.compareTo(BigDecimal.ZERO) < 0) {
            expectedAmountPaid = BigDecimal.ZERO;
        }
        if (validatedAmountPaid.compareTo(expectedAmountPaid) != 0) {
            throw new PaymentAmountMismatchException(expectedAmountPaid, validatedAmountPaid);
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

    private static List<PaymentMethodItem> validatePaymentMethodItems(
            List<PaymentMethodItem> items,
            BigDecimal amountPaid,
            PaymentMethod paymentMethod,
            UUID paymentId,
            Instant paidAt) {
        if (amountPaid != null && amountPaid.compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }
        if (items == null || items.isEmpty()) {
            if (paymentMethod != null && paymentMethod != PaymentMethod.MULTIPLE) {
                String defaultRef = paymentMethod == PaymentMethod.BANK_TRANSFER
                        ? "REF-" + (paymentId != null ? paymentId.toString().substring(0, 8) : "LEGACY")
                        : null;
                return List.of(PaymentMethodItem.create(
                        UUID.randomUUID(),
                        paymentId,
                        paymentMethod,
                        amountPaid,
                        defaultRef,
                        paidAt));
            }
            throw new ValidationException("At least one payment method item is required.");
        }

        BigDecimal sumItems = items.stream()
                .map(PaymentMethodItem::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sumItems.compareTo(amountPaid) != 0) {
            throw new PaymentAmountMismatchException(amountPaid, sumItems);
        }

        return List.copyOf(items);
    }

    private static PaymentMethod resolvePaymentMethod(List<PaymentMethodItem> items) {
        if (items == null || items.isEmpty()) {
            return PaymentMethod.CASH;
        }
        Set<PaymentMethod> distinctMethods = items.stream()
                .map(PaymentMethodItem::getPaymentMethod)
                .collect(Collectors.toSet());
        if (distinctMethods.size() == 1) {
            return distinctMethods.iterator().next();
        }
        return PaymentMethod.MULTIPLE;
    }

    public BigDecimal getAmountByMethod(PaymentMethod method) {
        if (method == null) {
            return BigDecimal.ZERO;
        }
        if (paymentMethodItems != null && !paymentMethodItems.isEmpty()) {
            return paymentMethodItems.stream()
                    .filter(item -> item.getPaymentMethod() == method)
                    .map(PaymentMethodItem::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        if (paymentMethod == method && amountPaid != null) {
            return amountPaid;
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getCashAmount() {
        return getAmountByMethod(PaymentMethod.CASH);
    }

    public BigDecimal getBankTransferAmount() {
        return getAmountByMethod(PaymentMethod.BANK_TRANSFER);
    }

    private static <T> T requireNonNull(T value, String message) {
        if (Objects.isNull(value)) {
            throw new ValidationException(message);
        }
        return value;
    }
}
