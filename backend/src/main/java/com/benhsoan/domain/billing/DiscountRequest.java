package com.benhsoan.domain.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.DiscountRequestStatus;
import com.benhsoan.domain.billing.enums.DiscountType;
import com.benhsoan.domain.billing.exception.DiscountExceedsTotalException;
import com.benhsoan.domain.billing.exception.InvalidDiscountStateException;
import com.benhsoan.domain.billing.exception.SelfApprovalNotAllowedException;
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
public class DiscountRequest {

    private UUID id;
    private UUID visitId;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String reason;
    private DiscountRequestStatus status;
    private UUID requestedBy;
    private Instant requestedAt;
    private UUID approvedBy;
    private Instant approvedAt;
    private UUID rejectedBy;
    private String rejectionReason;
    private Instant rejectedAt;
    private UUID invoiceId;

    private DiscountRequest(
            UUID id,
            UUID visitId,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal originalAmount,
            BigDecimal discountAmount,
            BigDecimal finalAmount,
            String reason,
            DiscountRequestStatus status,
            UUID requestedBy,
            Instant requestedAt,
            UUID approvedBy,
            Instant approvedAt,
            UUID rejectedBy,
            String rejectionReason,
            Instant rejectedAt,
            UUID invoiceId
    ) {
        this.id = requireNonNull(id, "ID yêu cầu giảm giá là bắt buộc.");
        this.visitId = requireNonNull(visitId, "Visit id là bắt buộc.");
        this.discountType = requireNonNull(discountType, "Loại giảm giá là bắt buộc.");
        this.discountValue = requireNonNull(discountValue, "Giá trị giảm giá là bắt buộc.");
        this.originalAmount = requireNonNull(originalAmount, "Số tiền gốc là bắt buộc.");
        this.discountAmount = requireNonNull(discountAmount, "Số tiền giảm là bắt buộc.");
        this.finalAmount = requireNonNull(finalAmount, "Số tiền sau giảm là bắt buộc.");
        this.reason = requireText(reason, "Lý do đề nghị giảm giá là bắt buộc.");
        this.status = requireNonNull(status, "Trạng thái đề nghị là bắt buộc.");
        this.requestedBy = requireNonNull(requestedBy, "Người đề nghị là bắt buộc.");
        this.requestedAt = requireNonNull(requestedAt, "Thời điểm đề nghị là bắt buộc.");
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
        this.rejectedBy = rejectedBy;
        this.rejectionReason = rejectionReason;
        this.rejectedAt = rejectedAt;
        this.invoiceId = invoiceId;
    }

    public static DiscountRequest create(
            UUID id,
            UUID visitId,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal originalAmount,
            String reason,
            UUID requestedBy,
            Instant requestedAt
    ) {
        requireNonNull(discountType, "Loại giảm giá là bắt buộc.");
        if (originalAmount == null || originalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Khoản phải thu gốc phải lớn hơn 0.");
        }

        BigDecimal calculatedDiscount;
        BigDecimal normalizedValue;

        switch (discountType) {
            case PERCENTAGE -> {
                if (discountValue == null
                        || discountValue.compareTo(BigDecimal.ZERO) <= 0
                        || discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new ValidationException("Tỷ lệ giảm giá phải lớn hơn 0% và không vượt quá 100%.");
                }
                normalizedValue = discountValue;
                calculatedDiscount = originalAmount
                        .multiply(discountValue)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
            case FIXED_AMOUNT -> {
                if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ValidationException("Số tiền giảm giá phải lớn hơn 0.");
                }
                if (discountValue.compareTo(originalAmount) > 0) {
                    throw new DiscountExceedsTotalException("Số tiền giảm giá không được vượt quá tổng khoản phải thu.");
                }
                normalizedValue = discountValue;
                calculatedDiscount = discountValue;
            }
            case FULL_FREE -> {
                normalizedValue = BigDecimal.valueOf(100);
                calculatedDiscount = originalAmount;
            }
            default -> throw new ValidationException("Loại giảm giá không hợp lệ.");
        }

        BigDecimal finalAmount = originalAmount.subtract(calculatedDiscount);

        return new DiscountRequest(
                id,
                visitId,
                discountType,
                normalizedValue,
                originalAmount,
                calculatedDiscount,
                finalAmount,
                reason,
                DiscountRequestStatus.PENDING,
                requestedBy,
                requestedAt,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public void approve(UUID approverId, Instant approvedAt) {
        if (approverId == null) {
            throw new ValidationException("ID người duyệt là bắt buộc.");
        }
        if (approvedAt == null) {
            throw new ValidationException("Thời điểm duyệt là bắt buộc.");
        }
        if (approverId.equals(this.requestedBy)) {
            throw new SelfApprovalNotAllowedException();
        }
        if (this.status != DiscountRequestStatus.PENDING) {
            throw new InvalidDiscountStateException("Chỉ có thể phê duyệt yêu cầu đang chờ duyệt (PENDING).");
        }
        this.status = DiscountRequestStatus.APPROVED;
        this.approvedBy = approverId;
        this.approvedAt = approvedAt;
    }

    public void reject(UUID rejecterId, String rejectionReason, Instant rejectedAt) {
        if (rejecterId == null) {
            throw new ValidationException("ID người từ chối là bắt buộc.");
        }
        if (rejectedAt == null) {
            throw new ValidationException("Thời điểm từ chối là bắt buộc.");
        }
        if (rejecterId.equals(this.requestedBy)) {
            throw new SelfApprovalNotAllowedException();
        }
        if (this.status != DiscountRequestStatus.PENDING) {
            throw new InvalidDiscountStateException("Chỉ có thể từ chối yêu cầu đang chờ duyệt (PENDING).");
        }
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new ValidationException("Lý do từ chối là bắt buộc.");
        }
        this.status = DiscountRequestStatus.REJECTED;
        this.rejectedBy = rejecterId;
        this.rejectionReason = rejectionReason.trim();
        this.rejectedAt = rejectedAt;
    }

    public void markApplied(UUID invoiceId) {
        if (invoiceId == null) {
            throw new ValidationException("ID hóa đơn là bắt buộc.");
        }
        if (this.status != DiscountRequestStatus.APPROVED) {
            throw new InvalidDiscountStateException("Chỉ có thể gắn hóa đơn cho đề nghị giảm giá đã được duyệt.");
        }
        this.invoiceId = invoiceId;
    }

    public static DiscountRequest restore(
            UUID id,
            UUID visitId,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal originalAmount,
            BigDecimal discountAmount,
            BigDecimal finalAmount,
            String reason,
            DiscountRequestStatus status,
            UUID requestedBy,
            Instant requestedAt,
            UUID approvedBy,
            Instant approvedAt,
            UUID rejectedBy,
            String rejectionReason,
            Instant rejectedAt,
            UUID invoiceId
    ) {
        return new DiscountRequest(
                id,
                visitId,
                discountType,
                discountValue,
                originalAmount,
                discountAmount,
                finalAmount,
                reason,
                status,
                requestedBy,
                requestedAt,
                approvedBy,
                approvedAt,
                rejectedBy,
                rejectionReason,
                rejectedAt,
                invoiceId
        );
    }

    public boolean isPending() {
        return status == DiscountRequestStatus.PENDING;
    }

    public boolean isApproved() {
        return status == DiscountRequestStatus.APPROVED;
    }

    public boolean isRejected() {
        return status == DiscountRequestStatus.REJECTED;
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
