package com.benhsoan.persistence.entity.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.CashierShiftStatus;

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
@Table(name = "cashier_shifts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashierShiftEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "shift_code", nullable = false, length = 30, unique = true)
    private String shiftCode;

    @Column(name = "cashier_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID cashierId;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "total_transactions", nullable = false)
    private int totalTransactions;

    @Column(name = "total_system_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalSystemAmount;

    @Column(name = "system_cash_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal systemCashAmount;

    @Column(name = "system_transfer_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal systemTransferAmount;

    @Column(name = "system_card_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal systemCardAmount;

    @Column(name = "system_other_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal systemOtherAmount;

    @Column(name = "actual_cash_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal actualCashAmount;

    @Column(name = "difference_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal differenceAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CashierShiftStatus status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "confirmed_by", columnDefinition = "BINARY(16)")
    private UUID confirmedBy;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "confirmation_notes", columnDefinition = "TEXT")
    private String confirmationNotes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
