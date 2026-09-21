package com.benhsoan.domain.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.CashierShiftStatus;
import com.benhsoan.domain.billing.exception.CashierShiftAlreadyConfirmedException;
import com.benhsoan.domain.billing.exception.CashierShiftNoteRequiredException;
import com.benhsoan.domain.billing.exception.SelfConfirmationNotAllowedException;
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
public class CashierShift {

    private UUID id;

    private String shiftCode;

    private UUID cashierId;

    private Instant startTime;

    private Instant endTime;

    private int totalTransactions;

    private BigDecimal totalSystemAmount;

    private BigDecimal systemCashAmount;

    private BigDecimal systemTransferAmount;

    private BigDecimal systemCardAmount;

    private BigDecimal systemOtherAmount;

    private BigDecimal actualCashAmount;

    private BigDecimal differenceAmount;

    private CashierShiftStatus status;

    private String notes;

    private UUID confirmedBy;

    private Instant confirmedAt;

    private String confirmationNotes;

    private Instant createdAt;

    private CashierShift(
            UUID id,
            String shiftCode,
            UUID cashierId,
            Instant startTime,
            Instant endTime,
            int totalTransactions,
            BigDecimal totalSystemAmount,
            BigDecimal systemCashAmount,
            BigDecimal systemTransferAmount,
            BigDecimal systemCardAmount,
            BigDecimal systemOtherAmount,
            BigDecimal actualCashAmount,
            BigDecimal differenceAmount,
            CashierShiftStatus status,
            String notes,
            UUID confirmedBy,
            Instant confirmedAt,
            String confirmationNotes,
            Instant createdAt
    ) {
        this.id = requireNonNull(id, "Mã định danh phiếu chốt ca không được để trống.");
        this.shiftCode = requireText(shiftCode, "Mã phiếu chốt ca không được để trống.");
        this.cashierId = requireNonNull(cashierId, "Mã thu ngân không được để trống.");
        this.startTime = requireNonNull(startTime, "Thời điểm bắt đầu ca không được để trống.");
        this.endTime = requireNonNull(endTime, "Thời điểm kết thúc ca không được để trống.");
        if (startTime.isAfter(endTime)) {
            throw new ValidationException("Thời điểm bắt đầu ca phải trước hoặc bằng thời điểm kết thúc ca.");
        }
        if (totalTransactions < 0) {
            throw new ValidationException("Tổng số giao dịch không được âm.");
        }
        this.totalTransactions = totalTransactions;
        this.totalSystemAmount = validateNonNegative(totalSystemAmount, "Tổng tiền hệ thống không được để trống.");
        this.systemCashAmount = validateNonNegative(systemCashAmount, "Tiền mặt hệ thống không được để trống.");
        this.systemTransferAmount = validateNonNegative(systemTransferAmount, "Tiền chuyển khoản hệ thống không được để trống.");
        this.systemCardAmount = validateNonNegative(systemCardAmount, "Tiền quẹt thẻ hệ thống không được để trống.");
        this.systemOtherAmount = validateNonNegative(systemOtherAmount, "Tiền phương thức khác hệ thống không được để trống.");
        this.actualCashAmount = validateNonNegative(actualCashAmount, "Tiền mặt thực tế không được để trống.");
        this.differenceAmount = requireNonNull(differenceAmount, "Chênh lệch tiền mặt không được để trống.");
        this.status = requireNonNull(status, "Trạng thái phiếu chốt ca không được để trống.");
        this.notes = notes != null ? notes.trim() : null;
        this.confirmedBy = confirmedBy;
        this.confirmedAt = confirmedAt;
        this.confirmationNotes = confirmationNotes != null ? confirmationNotes.trim() : null;
        this.createdAt = requireNonNull(createdAt, "Thời điểm tạo phiếu không được để trống.");
    }

    public static CashierShift create(
            UUID id,
            String shiftCode,
            UUID cashierId,
            Instant startTime,
            Instant endTime,
            int totalTransactions,
            BigDecimal totalSystemAmount,
            BigDecimal systemCashAmount,
            BigDecimal systemTransferAmount,
            BigDecimal systemCardAmount,
            BigDecimal systemOtherAmount,
            BigDecimal actualCashAmount,
            String notes,
            Instant createdAt
    ) {
        BigDecimal validatedActual = validateNonNegative(actualCashAmount, "Tiền mặt thực tế không được để trống.");
        BigDecimal validatedSysCash = validateNonNegative(systemCashAmount, "Tiền mặt hệ thống không được để trống.");
        BigDecimal difference = validatedActual.subtract(validatedSysCash);

        String trimmedNotes = notes != null ? notes.trim() : null;
        CashierShiftStatus initialStatus;

        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            if (trimmedNotes == null || trimmedNotes.isBlank()) {
                throw new CashierShiftNoteRequiredException();
            }
            initialStatus = CashierShiftStatus.PENDING_CONFIRMATION;
        } else {
            initialStatus = CashierShiftStatus.CONFIRMED;
        }

        return new CashierShift(
                id,
                shiftCode,
                cashierId,
                startTime,
                endTime,
                totalTransactions,
                totalSystemAmount,
                validatedSysCash,
                systemTransferAmount,
                systemCardAmount,
                systemOtherAmount,
                validatedActual,
                difference,
                initialStatus,
                trimmedNotes,
                null,
                null,
                null,
                createdAt
        );
    }

    public static CashierShift restore(
            UUID id,
            String shiftCode,
            UUID cashierId,
            Instant startTime,
            Instant endTime,
            int totalTransactions,
            BigDecimal totalSystemAmount,
            BigDecimal systemCashAmount,
            BigDecimal systemTransferAmount,
            BigDecimal systemCardAmount,
            BigDecimal systemOtherAmount,
            BigDecimal actualCashAmount,
            BigDecimal differenceAmount,
            CashierShiftStatus status,
            String notes,
            UUID confirmedBy,
            Instant confirmedAt,
            String confirmationNotes,
            Instant createdAt
    ) {
        return new CashierShift(
                id,
                shiftCode,
                cashierId,
                startTime,
                endTime,
                totalTransactions,
                totalSystemAmount,
                systemCashAmount,
                systemTransferAmount,
                systemCardAmount,
                systemOtherAmount,
                actualCashAmount,
                differenceAmount,
                status,
                notes,
                confirmedBy,
                confirmedAt,
                confirmationNotes,
                createdAt
        );
    }

    public void confirm(UUID managerId, Instant confirmedAt, String confirmationNotes) {
        if (this.status == CashierShiftStatus.CONFIRMED) {
            throw new CashierShiftAlreadyConfirmedException(this.id);
        }
        if (this.cashierId != null && this.cashierId.equals(managerId)) {
            throw new SelfConfirmationNotAllowedException();
        }
        this.status = CashierShiftStatus.CONFIRMED;
        this.confirmedBy = requireNonNull(managerId, "Mã quản lý xác nhận không được để trống.");
        this.confirmedAt = requireNonNull(confirmedAt, "Thời điểm xác nhận không được để trống.");
        this.confirmationNotes = confirmationNotes != null ? confirmationNotes.trim() : null;
    }

    public void reject(UUID managerId, Instant confirmedAt, String confirmationNotes) {
        this.status = CashierShiftStatus.REJECTED;
        this.confirmedBy = requireNonNull(managerId, "Mã quản lý xác nhận không được để trống.");
        this.confirmedAt = requireNonNull(confirmedAt, "Thời điểm xác nhận không được để trống.");
        this.confirmationNotes = confirmationNotes != null ? confirmationNotes.trim() : null;
    }

    public boolean isConfirmed() {
        return this.status == CashierShiftStatus.CONFIRMED;
    }

    private static BigDecimal validateNonNegative(BigDecimal value, String message) {
        if (value == null) {
            throw new ValidationException(message);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(message.replace("không được để trống", "không được âm"));
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
