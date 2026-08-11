package com.benhsoan.domain.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.domain.billing.exception.InvoiceAdjustmentReasonRequiredException;
import com.benhsoan.domain.billing.exception.InvoiceAlreadyIssuedException;
import com.benhsoan.domain.billing.exception.InvoiceUnauthorizedAdjustmentException;
import com.benhsoan.domain.billing.exception.PaymentRequiredForInvoiceException;
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
public class Invoice {

    private UUID id;

    private String invoiceCode;

    private UUID visitId;

    private UUID paymentId;

    private InvoiceType type;

    private UUID originalInvoiceId;

    private String adjustmentReason;

    private BigDecimal totalAmount;

    private UUID createdBy;

    private Instant createdAt;

    private List<InvoiceLine> lines;

    private Invoice(
            UUID id,
            String invoiceCode,
            UUID visitId,
            UUID paymentId,
            InvoiceType type,
            UUID originalInvoiceId,
            String adjustmentReason,
            BigDecimal totalAmount,
            UUID createdBy,
            Instant createdAt,
            List<InvoiceLine> lines
    ) {
        this.id = requireNonNull(id, "Invoice id is required.");
        this.invoiceCode = requireText(invoiceCode, "Invoice code is required.");
        this.visitId = requireNonNull(visitId, "Visit id is required.");
        this.paymentId = paymentId;
        this.type = requireNonNull(type, "Invoice type is required.");
        this.originalInvoiceId = originalInvoiceId;
        this.adjustmentReason = normalizeOptionalText(adjustmentReason);
        this.createdBy = requireNonNull(createdBy, "Invoice creator id is required.");
        this.createdAt = requireNonNull(createdAt, "Invoice creation time is required.");
        this.lines = validateAndCopyLines(lines, id);
        this.totalAmount = validateTotalAmount(totalAmount, this.lines);
        validateInvoiceTypeConsistency();
    }

    public static Invoice createOriginal(
            UUID id,
            String invoiceCode,
            UUID visitId,
            UUID paymentId,
            UUID createdBy,
            Instant createdAt,
            List<InvoiceLine> lines,
            boolean paymentRecorded,
            boolean invoiceAlreadyExists
    ) {
        if (!paymentRecorded) {
            throw new PaymentRequiredForInvoiceException();
        }
        if (invoiceAlreadyExists) {
            throw new InvoiceAlreadyIssuedException(visitId);
        }

        BigDecimal totalAmount = sumLineAmounts(lines);
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Original invoice total amount must be greater than zero.");
        }

        return new Invoice(
                id,
                invoiceCode,
                visitId,
                paymentId,
                InvoiceType.ORIGINAL,
                null,
                null,
                totalAmount,
                createdBy,
                createdAt,
                lines
        );
    }

    public static Invoice createAdjustment(
            UUID id,
            String invoiceCode,
            UUID visitId,
            UUID originalInvoiceId,
            String adjustmentReason,
            UUID createdBy,
            Instant createdAt,
            List<InvoiceLine> lines,
            boolean managerAuthorized
    ) {
        if (!managerAuthorized) {
            throw new InvoiceUnauthorizedAdjustmentException();
        }
        if (adjustmentReason == null || adjustmentReason.isBlank()) {
            throw new InvoiceAdjustmentReasonRequiredException();
        }

        BigDecimal totalAmount = sumLineAmounts(lines);
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new ValidationException("Adjustment invoice total amount must not be zero.");
        }

        return new Invoice(
                id,
                invoiceCode,
                visitId,
                null,
                InvoiceType.ADJUSTMENT,
                requireNonNull(originalInvoiceId, "Original invoice id is required."),
                adjustmentReason,
                totalAmount,
                createdBy,
                createdAt,
                lines
        );
    }

    public static Invoice restore(
            UUID id,
            String invoiceCode,
            UUID visitId,
            UUID paymentId,
            InvoiceType type,
            UUID originalInvoiceId,
            String adjustmentReason,
            BigDecimal totalAmount,
            UUID createdBy,
            Instant createdAt,
            List<InvoiceLine> lines
    ) {
        return new Invoice(
                id,
                invoiceCode,
                visitId,
                paymentId,
                type,
                originalInvoiceId,
                adjustmentReason,
                totalAmount,
                createdBy,
                createdAt,
                lines
        );
    }

    public boolean isOriginal() {
        return type == InvoiceType.ORIGINAL;
    }

    public boolean isAdjustment() {
        return type == InvoiceType.ADJUSTMENT;
    }

    private void validateInvoiceTypeConsistency() {
        if (type == InvoiceType.ORIGINAL) {
            if (paymentId == null) {
                throw new PaymentRequiredForInvoiceException();
            }
            if (originalInvoiceId != null) {
                throw new ValidationException("Original invoice must not reference another invoice.");
            }
            if (adjustmentReason != null) {
                throw new ValidationException("Original invoice must not contain an adjustment reason.");
            }
            if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Original invoice total amount must be greater than zero.");
            }
            return;
        }

        if (originalInvoiceId == null) {
            throw new ValidationException("Adjustment invoice must reference the original invoice.");
        }
        if (adjustmentReason == null) {
            throw new InvoiceAdjustmentReasonRequiredException();
        }
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new ValidationException("Adjustment invoice total amount must not be zero.");
        }
    }

    private static List<InvoiceLine> validateAndCopyLines(
            List<InvoiceLine> lines,
            UUID invoiceId
    ) {
        if (lines == null || lines.isEmpty()) {
            throw new ValidationException("Invoice must contain at least one line.");
        }

        for (InvoiceLine line : lines) {
            if (line == null) {
                throw new ValidationException("Invoice line is required.");
            }
            if (!invoiceId.equals(line.getInvoiceId())) {
                throw new ValidationException(
                        "Invoice line does not belong to invoice: " + invoiceId
                );
            }
        }

        return List.copyOf(lines);
    }

    private static BigDecimal validateTotalAmount(
            BigDecimal totalAmount,
            List<InvoiceLine> lines
    ) {
        if (totalAmount == null) {
            throw new ValidationException("Invoice total amount is required.");
        }

        BigDecimal expectedTotal = sumLineAmounts(lines);
        if (totalAmount.compareTo(expectedTotal) != 0) {
            throw new ValidationException("Invoice total amount must equal the sum of invoice lines.");
        }
        return totalAmount;
    }

    private static BigDecimal sumLineAmounts(List<InvoiceLine> lines) {
        if (lines == null) {
            throw new ValidationException("Invoice lines are required.");
        }
        return lines.stream()
                .map(InvoiceLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
