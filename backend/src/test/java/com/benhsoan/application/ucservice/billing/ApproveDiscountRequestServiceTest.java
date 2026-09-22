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
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.billing.DiscountRequest;
import com.benhsoan.domain.billing.enums.DiscountRequestStatus;
import com.benhsoan.domain.billing.enums.DiscountType;
import com.benhsoan.domain.billing.exception.SelfApprovalNotAllowedException;
import com.benhsoan.port.dto.result.DiscountRequestResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class ApproveDiscountRequestServiceTest {

    @Test
    void approvesDiscountRequestSuccessfully() {
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

        ApproveDiscountRequestService service = new ApproveDiscountRequestService(
                repository,
                currentUserPort,
                clockPort,
                auditLogRepository,
                accessDeniedAuditWriter,
                new DiscountRequestResultMapper()
        );

        DiscountRequestResult result = service.approve(requestId);

        assertEquals(DiscountRequestStatus.APPROVED, result.status());
        assertEquals(managerId, result.approvedBy());
        assertEquals(now, result.approvedAt());
        verify(repository).findByIdForUpdate(requestId);
        verify(repository).save(any(DiscountRequest.class));
        verify(auditLogRepository).save(any());
    }

    @Test
    void blocksSelfApprovalAndRecordsAccessDeniedAudit() {
        DiscountRequestRepository repository = mock(DiscountRequestRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
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

        ApproveDiscountRequestService service = new ApproveDiscountRequestService(
                repository,
                currentUserPort,
                clockPort,
                auditLogRepository,
                accessDeniedAuditWriter,
                new DiscountRequestResultMapper()
        );

        assertThrows(SelfApprovalNotAllowedException.class, () -> service.approve(requestId));

        verify(accessDeniedAuditWriter).recordAccessDenied(
                eq(userId),
                eq(ResourceType.DISCOUNT_REQUEST),
                eq(requestId),
                eq("Người yêu cầu không được tự phê duyệt đề nghị giảm giá của chính mình."),
                eq(now)
        );
    }

    @Test
    void blocksAdminSelfApprovalAndRecordsAccessDeniedAudit() {
        DiscountRequestRepository repository = mock(DiscountRequestRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        BillingAccessDeniedAuditWriter accessDeniedAuditWriter = mock(BillingAccessDeniedAuditWriter.class);

        UUID requestId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-21T11:00:00Z");

        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(adminId);
        when(clockPort.now()).thenReturn(now);

        DiscountRequest request = DiscountRequest.create(
                requestId,
                UUID.randomUUID(),
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("30000"),
                new BigDecimal("100000"),
                "Lý do",
                adminId,
                now.minusSeconds(3600)
        );
        when(repository.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));

        ApproveDiscountRequestService service = new ApproveDiscountRequestService(
                repository,
                currentUserPort,
                clockPort,
                auditLogRepository,
                accessDeniedAuditWriter,
                new DiscountRequestResultMapper()
        );

        assertThrows(SelfApprovalNotAllowedException.class, () -> service.approve(requestId));

        verify(accessDeniedAuditWriter).recordAccessDenied(
                eq(adminId),
                eq(ResourceType.DISCOUNT_REQUEST),
                eq(requestId),
                eq("Người yêu cầu không được tự phê duyệt đề nghị giảm giá của chính mình."),
                eq(now)
        );
    }

    @Test
    void rejectsUnauthorizedApproverAndRecordsAudit() {
        DiscountRequestRepository repository = mock(DiscountRequestRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);
        BillingAccessDeniedAuditWriter accessDeniedAuditWriter = mock(BillingAccessDeniedAuditWriter.class);

        UUID requestId = UUID.randomUUID();
        UUID receptionistId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-21T11:00:00Z");

        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(false);
        when(currentUserPort.getCurrentUserId()).thenReturn(receptionistId);
        when(clockPort.now()).thenReturn(now);

        ApproveDiscountRequestService service = new ApproveDiscountRequestService(
                repository,
                currentUserPort,
                clockPort,
                mock(AuditLogRepository.class),
                accessDeniedAuditWriter,
                new DiscountRequestResultMapper()
        );

        assertThrows(AccessDeniedException.class, () -> service.approve(requestId));

        verify(accessDeniedAuditWriter).recordAccessDenied(
                eq(receptionistId),
                eq(ResourceType.DISCOUNT_REQUEST),
                eq(requestId),
                eq("Chỉ người quản lý hoặc quản trị viên mới có quyền duyệt đề nghị giảm giá."),
                eq(now)
        );
    }
}
