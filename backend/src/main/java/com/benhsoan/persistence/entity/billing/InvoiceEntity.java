package com.benhsoan.persistence.entity.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.InvoiceType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "invoice_code", nullable = false, unique = true, length = 30)
    private String invoiceCode;

    @Column(name = "visit_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID visitId;

    @Column(name = "payment_id", columnDefinition = "BINARY(16)")
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false, length = 30)
    private InvoiceType type;

    @Column(name = "original_invoice_id", columnDefinition = "BINARY(16)")
    private UUID originalInvoiceId;

    @Column(name = "adjustment_reason", columnDefinition = "TEXT")
    private String adjustmentReason;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
