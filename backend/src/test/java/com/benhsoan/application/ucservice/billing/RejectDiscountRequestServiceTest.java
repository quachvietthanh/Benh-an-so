package com.benhsoan.application.ucservice.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.billing.DiscountRequest;
import com.benhsoan.domain.billing.enums.DiscountRequestStatus;
import com.benhsoan.domain.billing.enums.DiscountType;
import com.benhsoan.domain.billing.exception.SelfApprovalNotAllowedException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.billing.RejectDiscountRequestCommand;
import com.benhsoan.port.dto.result.DiscountRequestResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class RejectDiscountRequestServiceTest {

    @Test
    void rejectsDiscountRequestSuccessfully() {
        DiscountRequestRepository repository = mock(DiscountRequestRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        BillingAccessDeniedAuditWriter accessDeniedAuditWriter = mock(BillingAccessDeniedAuditWriter.class);

        UUID requestId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-21T11:00:00Z");

        when(currentUserPort.hasRole("MANAGER")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(managerId);
        when(clockPort.now()).thenReturn(now);

        DiscountRequest request = DiscountRequest.create(
                requestId,
                UUID.randomUUID(),
                DiscountType.PERCENTAGE,
                new BigDecimal("20"),
                new BigDecimal("100000"),
                "Lý do",
                requesterId,
                now.minusSeconds(3600)
        );
        when(repository.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
        when(repository.save(any(DiscountRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        RejectDiscountRequestService service = new RejectDiscountRequestService(
                repository,
                currentUserPort,
                clockPort,
                auditLogRepository,
                accessDeniedAuditWriter,
                new DiscountRequestResultMapper()
        );

        RejectDiscountRequestCommand command = new RejectDiscountRequestCommand(
                requestId,
                "Không đủ điều kiện hưởng chính sách"
        );

        DiscountRequestResult result = service.reject(command);

        assertEquals(DiscountRequestStatus.REJECTED, result.status());
        assertEquals(managerId, result.rejectedBy());
        assertEquals("Không đủ điều kiện hưởng chính sách", result.rejectionReason());
        verify(repository).save(any(DiscountRequest.class));
        verify(auditLogRepository).save(any());
    }

    @Test
    void blocksSelfRejectionAndRecordsAccessDeniedAudit() {
        DiscountRequestRepository repository = mock(DiscountRequestRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);
        BillingAccessDeniedAuditWriter accessDeniedAuditWriter = mock(BillingAccessDeniedAuditWriter.class);

        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-21T11:00:00Z");

        when(currentUserPort.hasRole("MANAGER")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(clockPort.now()).thenReturn(now);

        DiscountRequest request = DiscountRequest.create(
                requestId,
                UUID.randomUUID(),
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("30000"),
                new BigDecimal("100000"),
                "Lý do",
                userId,
                now.minusSeconds(3600)
        );
        when(repository.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));

        RejectDiscountRequestService service = new RejectDiscountRequestService(
                repository,
                currentUserPort,
                clockPort,
                mock(AuditLogRepository.class),
                accessDeniedAuditWriter,
                new DiscountRequestResultMapper()
        );

        RejectDiscountRequestCommand command = new RejectDiscountRequestCommand(
                requestId,
                "Tự từ chối"
        );

        assertThrows(SelfApprovalNotAllowedException.class, () -> service.reject(command));

        verify(accessDeniedAuditWriter).recordAccessDenied(
                eq(userId),
                eq(ResourceType.DISCOUNT_REQUEST),
                eq(requestId),
                eq("Người yêu cầu không được tự xử lý đề nghị giảm giá của chính mình."),
                eq(now)
        );
    }

    @Test
    void rejectsEmptyRejectionReason() {
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());

        RejectDiscountRequestService service = new RejectDiscountRequestService(
                mock(DiscountRequestRepository.class),
                currentUserPort,
                mock(ClockPort.class),
                mock(AuditLogRepository.class),
                mock(BillingAccessDeniedAuditWriter.class),
                new DiscountRequestResultMapper()
        );

        RejectDiscountRequestCommand command = new RejectDiscountRequestCommand(
                UUID.randomUUID(),
                "   "
        );

        assertThrows(ValidationException.class, () -> service.reject(command));
    }
}
