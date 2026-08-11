package com.benhsoan.domain.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.InvoiceLineType;
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
public class InvoiceLine {

    private UUID id;

    private UUID invoiceId;

    private InvoiceLineType lineType;

    private String itemName;

    private UUID referenceId;

    private int quantity;

    private BigDecimal unitPrice;

    private BigDecimal amount;

    private Instant createdAt;

    private InvoiceLine(
            UUID id,
            UUID invoiceId,
            InvoiceLineType lineType,
            String itemName,
            UUID referenceId,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal amount,
            Instant createdAt
    ) {
        this.id = requireNonNull(id, "Invoice line id is required.");
        this.invoiceId = requireNonNull(invoiceId, "Invoice id is required.");
        this.lineType = requireNonNull(lineType, "Invoice line type is required.");
        this.itemName = requireText(itemName, "Invoice line item name is required.");
        this.referenceId = referenceId;
        this.quantity = validateQuantity(quantity);
        this.unitPrice = validateMoney(unitPrice, "Invoice line unit price is required.");
        this.amount = validateAmount(amount, this.unitPrice, this.quantity);
        this.createdAt = requireNonNull(createdAt, "Invoice line creation time is required.");
    }

    public static InvoiceLine create(
            UUID id,
            UUID invoiceId,
            InvoiceLineType lineType,
            String itemName,
            UUID referenceId,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal amount,
            Instant createdAt
    ) {
        return new InvoiceLine(
                id,
                invoiceId,
                lineType,
                itemName,
                referenceId,
                quantity,
                unitPrice,
                amount,
                createdAt
        );
    }

    private static int validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new ValidationException("Invoice line quantity must be greater than zero.");
        }
        return quantity;
    }

    private static BigDecimal validateAmount(
            BigDecimal amount,
            BigDecimal unitPrice,
            int quantity
    ) {
        BigDecimal validatedAmount = validateMoney(amount, "Invoice line amount is required.");
        BigDecimal expectedAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        if (validatedAmount.compareTo(expectedAmount) != 0) {
            throw new ValidationException("Invoice line amount must equal quantity multiplied by unit price.");
        }
        return validatedAmount;
    }

    private static BigDecimal validateMoney(BigDecimal value, String message) {
        if (value == null) {
            throw new ValidationException(message);
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
