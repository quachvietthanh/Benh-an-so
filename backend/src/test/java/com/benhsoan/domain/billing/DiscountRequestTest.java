package com.benhsoan.domain.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.billing.enums.DiscountRequestStatus;
import com.benhsoan.domain.billing.enums.DiscountType;
import com.benhsoan.domain.billing.exception.DiscountExceedsTotalException;
import com.benhsoan.domain.billing.exception.InvalidDiscountStateException;
import com.benhsoan.domain.billing.exception.SelfApprovalNotAllowedException;
import com.benhsoan.domain.shared.exception.ValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Domain: DiscountRequest Aggregate Tests")
class DiscountRequestTest {

    private final UUID visitId = UUID.randomUUID();
    private final UUID receptionistId = UUID.randomUUID();
    private final UUID managerId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-21T10:00:00Z");

    @Nested
    @DisplayName("Tạo đề nghị giảm giá (Creation)")
    class CreationTests {

        @Test
        @DisplayName("Tạo thành công đề nghị giảm giá theo tỷ lệ phần trăm (PERCENTAGE)")
        void shouldCreatePercentageDiscountRequestSuccessfully() {
            DiscountRequest request = DiscountRequest.create(
                    UUID.randomUUID(),
                    visitId,
                    DiscountType.PERCENTAGE,
                    new BigDecimal("20"),
                    new BigDecimal("200000"),
                    "Ưu đãi khách hàng thân thiết",
                    receptionistId,
                    now
            );

            assertThat(request.getStatus()).isEqualTo(DiscountRequestStatus.PENDING);
            assertThat(request.getDiscountType()).isEqualTo(DiscountType.PERCENTAGE);
            assertThat(request.getDiscountValue()).isEqualByComparingTo("20");
            assertThat(request.getOriginalAmount()).isEqualByComparingTo("200000");
            assertThat(request.getDiscountAmount()).isEqualByComparingTo("40000");
            assertThat(request.getFinalAmount()).isEqualByComparingTo("160000");
            assertThat(request.getRequestedBy()).isEqualTo(receptionistId);
            assertThat(request.getRequestedAt()).isEqualTo(now);
            assertThat(request.isPending()).isTrue();
        }

        @Test
        @DisplayName("Tạo thành công đề nghị giảm giá theo số tiền cố định (FIXED_AMOUNT)")
        void shouldCreateFixedAmountDiscountRequestSuccessfully() {
            DiscountRequest request = DiscountRequest.create(
                    UUID.randomUUID(),
                    visitId,
                    DiscountType.FIXED_AMOUNT,
                    new BigDecimal("50000"),
                    new BigDecimal("200000"),
                    "Hỗ trợ chi phí khám",
                    receptionistId,
                    now
            );

            assertThat(request.getStatus()).isEqualTo(DiscountRequestStatus.PENDING);
            assertThat(request.getDiscountType()).isEqualTo(DiscountType.FIXED_AMOUNT);
            assertThat(request.getDiscountValue()).isEqualByComparingTo("50000");
            assertThat(request.getDiscountAmount()).isEqualByComparingTo("50000");
            assertThat(request.getFinalAmount()).isEqualByComparingTo("150000");
        }

        @Test
        @DisplayName("Tạo thành công đề nghị miễn phí 100% (FULL_FREE)")
        void shouldCreateFullFreeDiscountRequestSuccessfully() {
            DiscountRequest request = DiscountRequest.create(
                    UUID.randomUUID(),
                    visitId,
                    DiscountType.FULL_FREE,
                    BigDecimal.ZERO,
                    new BigDecimal("350000"),
                    "Miễn phí khám đối tượng chính sách",
                    receptionistId,
                    now
            );

            assertThat(request.getStatus()).isEqualTo(DiscountRequestStatus.PENDING);
            assertThat(request.getDiscountType()).isEqualTo(DiscountType.FULL_FREE);
            assertThat(request.getDiscountValue()).isEqualByComparingTo("100");
            assertThat(request.getDiscountAmount()).isEqualByComparingTo("350000");
            assertThat(request.getFinalAmount()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("Ném lỗi khi số tiền cố định vượt quá tổng số tiền phải thu")
        void shouldThrowExceptionWhenFixedDiscountExceedsTotal() {
            assertThatThrownBy(() -> DiscountRequest.create(
                    UUID.randomUUID(),
                    visitId,
                    DiscountType.FIXED_AMOUNT,
                    new BigDecimal("250000"),
                    new BigDecimal("200000"),
                    "Giảm nhiều hơn tổng tiền",
                    receptionistId,
                    now
            ))
                    .isInstanceOf(DiscountExceedsTotalException.class)
                    .hasMessageContaining("không được vượt quá");
        }

        @Test
        @DisplayName("Ném lỗi khi tỷ lệ phần trăm không hợp lệ (<= 0 hoặc > 100)")
        void shouldThrowExceptionWhenPercentageIsInvalid() {
            assertThatThrownBy(() -> DiscountRequest.create(
                    UUID.randomUUID(),
                    visitId,
                    DiscountType.PERCENTAGE,
                    BigDecimal.ZERO,
                    new BigDecimal("200000"),
                    "Lý do",
                    receptionistId,
                    now
            ))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Tỷ lệ giảm giá phải lớn hơn 0%");

            assertThatThrownBy(() -> DiscountRequest.create(
                    UUID.randomUUID(),
                    visitId,
                    DiscountType.PERCENTAGE,
                    new BigDecimal("105"),
                    new BigDecimal("200000"),
                    "Lý do",
                    receptionistId,
                    now
            ))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("không vượt quá 100%");
        }

        @Test
        @DisplayName("Ném lỗi khi số tiền gốc <= 0 hoặc lý do trống")
        void shouldThrowExceptionWhenOriginalAmountOrReasonIsInvalid() {
            assertThatThrownBy(() -> DiscountRequest.create(
                    UUID.randomUUID(),
                    visitId,
                    DiscountType.PERCENTAGE,
                    new BigDecimal("10"),
                    BigDecimal.ZERO,
                    "Lý do",
                    receptionistId,
                    now
            ))
                    .isInstanceOf(ValidationException.class);

            assertThatThrownBy(() -> DiscountRequest.create(
                    UUID.randomUUID(),
                    visitId,
                    DiscountType.PERCENTAGE,
                    new BigDecimal("10"),
                    new BigDecimal("100000"),
                    "   ",
                    receptionistId,
                    now
            ))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("Phê duyệt đề nghị (Approval) - QTN-37")
    class ApprovalTests {

        @Test
        @DisplayName("Quản lý duyệt đề nghị hợp lệ thành công")
        void shouldApproveSuccessfully() {
            DiscountRequest request = DiscountRequest.create(
                    UUID.randomUUID(),
                    visitId,
                    DiscountType.PERCENTAGE,
                    new BigDecimal("15"),
                    new BigDecimal("200000"),
                    "Lý do ưu đãi",
                    receptionistId,
                    now
            );

            Instant approvedAt = now.plusSeconds(300);
            request.approve(managerId, approvedAt);

            assertThat(request.getStatus()).isEqualTo(DiscountRequestStatus.APPROVED);
            assertThat(request.getApprovedBy()).isEqualTo(managerId);
            assertThat(request.getApprovedAt()).isEqualTo(approvedAt);
            assertThat(request.isApproved()).isTrue();
        }

        @Test
        @DisplayName("Chặn người đề nghị tự duyệt đề nghị của mình (QTN-37, AC TC-03)")
        void shouldThrowExceptionWhenApproverIsRequester() {
            DiscountRequest request = DiscountRequest.create(
                    UUID.randomUUID(),
                    visitId,
                    DiscountType.PERCENTAGE,
                    new BigDecimal("15"),
                    new BigDecimal("200000"),
                    "Lý do",
                    receptionistId,
                    now
            );

            assertThatThrownBy(() -> request.approve(receptionistId, now.plusSeconds(60)))
                    .isInstanceOf(SelfApprovalNotAllowedException.class);
        }

        @Test
        @DisplayName("Ném lỗi khi phê duyệt đề nghị đã duyệt hoặc đã từ chối")
        void shouldThrowExceptionWhenApprovingNonPendingRequest() {
            DiscountRequest request = DiscountRequest.create(
                    UUID.randomUUID(),
                    visitId,
                    DiscountType.PERCENTAGE,
                    new BigDecimal("15"),
                    new BigDecimal("200000"),
                    "Lý do",
                    receptionistId,
                    now
            );
            request.approve(managerId, now.plusSeconds(60));

            assertThatThrownBy(() -> request.approve(UUID.randomUUID(), now.plusSeconds(120)))
                    .isInstanceOf(InvalidDiscountStateException.class);
        }
    }

    @Nested
    @DisplayName("Từ chối đề nghị (Rejection)")
    class RejectionTests {

        @Test
        @DisplayName("Từ chối đề nghị thành công kèm lý do")
        void shouldRejectSuccessfully() {
            DiscountRequest request = DiscountRequest.create(
                    UUID.randomUUID(),
                    visitId,
                    DiscountType.FIXED_AMOUNT,
                    new BigDecimal("50000"),
                    new BigDecimal("200000"),
                    "Lý do",
                    receptionistId,
                    now
            );

            Instant rejectedAt = now.plusSeconds(300);
            request.reject(managerId, "Không thuộc diện ưu đãi quy định", rejectedAt);

            assertThat(request.getStatus()).isEqualTo(DiscountRequestStatus.REJECTED);
            assertThat(request.getRejectedBy()).isEqualTo(managerId);
            assertThat(request.getRejectedAt()).isEqualTo(rejectedAt);
            assertThat(request.getRejectionReason()).isEqualTo("Không thuộc diện ưu đãi quy định");
            assertThat(request.isRejected()).isTrue();
        }

        @Test
        @DisplayName("Chặn người đề nghị tự từ chối chính mình qua API duyệt")
        void shouldThrowExceptionWhenRejecterIsRequester() {
            DiscountRequest request = DiscountRequest.create(
                    UUID.randomUUID(),
                    visitId,
                    DiscountType.FIXED_AMOUNT,
                    new BigDecimal("50000"),
                    new BigDecimal("200000"),
                    "Lý do",
                    receptionistId,
                    now
            );

            assertThatThrownBy(() -> request.reject(receptionistId, "Lý do", now.plusSeconds(60)))
                    .isInstanceOf(SelfApprovalNotAllowedException.class);
        }

        @Test
        @DisplayName("Ném lỗi khi từ chối không kèm lý do")
        void shouldThrowExceptionWhenRejectionReasonIsBlank() {
            DiscountRequest request = DiscountRequest.create(
                    UUID.randomUUID(),
                    visitId,
                    DiscountType.FIXED_AMOUNT,
                    new BigDecimal("50000"),
                    new BigDecimal("200000"),
                    "Lý do",
                    receptionistId,
                    now
            );

            assertThatThrownBy(() -> request.reject(managerId, "   ", now.plusSeconds(60)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Lý do từ chối");
        }
    }

    @Nested
    @DisplayName("Gắn hóa đơn (Mark Applied)")
    class MarkAppliedTests {

        @Test
        @DisplayName("Gắn ID hóa đơn thành công khi đề nghị đã duyệt")
        void shouldMarkAppliedSuccessfully() {
            DiscountRequest request = DiscountRequest.create(
                    UUID.randomUUID(),
                    visitId,
                    DiscountType.PERCENTAGE,
                    new BigDecimal("10"),
                    new BigDecimal("100000"),
                    "Lý do",
                    receptionistId,
                    now
            );
            request.approve(managerId, now.plusSeconds(60));

            UUID invoiceId = UUID.randomUUID();
            request.markApplied(invoiceId);

            assertThat(request.getInvoiceId()).isEqualTo(invoiceId);
        }

        @Test
        @DisplayName("Ném lỗi khi gắn hóa đơn cho đề nghị chưa duyệt")
        void shouldThrowExceptionWhenMarkingAppliedOnPendingRequest() {
            DiscountRequest request = DiscountRequest.create(
                    UUID.randomUUID(),
                    visitId,
                    DiscountType.PERCENTAGE,
                    new BigDecimal("10"),
                    new BigDecimal("100000"),
                    "Lý do",
                    receptionistId,
                    now
            );

            assertThatThrownBy(() -> request.markApplied(UUID.randomUUID()))
                    .isInstanceOf(InvalidDiscountStateException.class);
        }
    }
}
