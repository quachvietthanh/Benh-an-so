package com.benhsoan.application.ucservice.billing;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.billing.DiscountRequest;
import com.benhsoan.domain.billing.exception.DiscountRequestNotFoundException;
import com.benhsoan.domain.billing.exception.SelfApprovalNotAllowedException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.billing.RejectDiscountRequestCommand;
import com.benhsoan.port.dto.result.DiscountRequestResult;
import com.benhsoan.port.inbound.billing.RejectDiscountRequestUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class RejectDiscountRequestService implements RejectDiscountRequestUseCase {

    private final DiscountRequestRepository discountRequestRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final AuditLogRepository auditLogRepository;
    private final BillingAccessDeniedAuditWriter accessDeniedAuditWriter;
    private final DiscountRequestResultMapper resultMapper;
    private final ObjectMapper objectMapper;

    @Autowired
    public RejectDiscountRequestService(
            DiscountRequestRepository discountRequestRepository,
            CurrentUserPort currentUserPort,
            ClockPort clockPort,
            AuditLogRepository auditLogRepository,
            BillingAccessDeniedAuditWriter accessDeniedAuditWriter,
            DiscountRequestResultMapper resultMapper,
            ObjectMapper objectMapper) {
        this.discountRequestRepository = discountRequestRepository;
        this.currentUserPort = currentUserPort;
        this.clockPort = clockPort;
        this.auditLogRepository = auditLogRepository;
        this.accessDeniedAuditWriter = accessDeniedAuditWriter;
        this.resultMapper = resultMapper;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public RejectDiscountRequestService(
            DiscountRequestRepository discountRequestRepository,
            CurrentUserPort currentUserPort,
            ClockPort clockPort,
            AuditLogRepository auditLogRepository,
            BillingAccessDeniedAuditWriter accessDeniedAuditWriter,
            DiscountRequestResultMapper resultMapper) {
        this(
                discountRequestRepository,
                currentUserPort,
                clockPort,
                auditLogRepository,
                accessDeniedAuditWriter,
                resultMapper,
                new ObjectMapper());
    }

    @Override
    public DiscountRequestResult reject(RejectDiscountRequestCommand command) {
        if (command == null || command.discountRequestId() == null) {
            throw new ValidationException("Mã đề nghị giảm giá là bắt buộc.");
        }
        if (command.rejectionReason() == null || command.rejectionReason().isBlank()) {
            throw new ValidationException("Lý do từ chối là bắt buộc.");
        }

        UUID actorId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();

        if (!currentUserPort.hasRole("ADMIN") && !currentUserPort.hasRole("MANAGER")) {
            accessDeniedAuditWriter.recordAccessDenied(
                    actorId,
                    ResourceType.DISCOUNT_REQUEST,
                    command.discountRequestId(),
                    "Chỉ người quản lý hoặc quản trị viên mới có quyền từ chối đề nghị giảm giá.",
                    now);
            throw new AccessDeniedException(
                    "Chỉ người quản lý hoặc quản trị viên mới có quyền từ chối đề nghị giảm giá.");
        }

        DiscountRequest request = discountRequestRepository.findByIdForUpdate(command.discountRequestId())
                .orElseThrow(() -> new DiscountRequestNotFoundException(command.discountRequestId()));

        if (actorId != null && actorId.equals(request.getRequestedBy())) {
            accessDeniedAuditWriter.recordAccessDenied(
                    actorId,
                    ResourceType.DISCOUNT_REQUEST,
                    request.getId(),
                    "Người yêu cầu không được tự xử lý đề nghị giảm giá của chính mình.",
                    now);
            throw new SelfApprovalNotAllowedException();
        }

        request.reject(actorId, command.rejectionReason(), now);

        DiscountRequest saved = discountRequestRepository.save(request);

        Map<String, Object> auditPayload = new LinkedHashMap<>();
        auditPayload.put("action", "REJECT");
        auditPayload.put("discountRequestId", saved.getId() != null ? saved.getId().toString() : null);
        auditPayload.put("visitId", saved.getVisitId() != null ? saved.getVisitId().toString() : null);
        auditPayload.put("rejectionReason", saved.getRejectionReason());

        String auditDetailsJson;
        try {
            auditDetailsJson = objectMapper.writeValueAsString(auditPayload);
        } catch (JsonProcessingException e) {
            auditDetailsJson = "{}";
        }

        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.UPDATE,
                ResourceType.DISCOUNT_REQUEST,
                saved.getId(),
                auditDetailsJson,
                null,
                now));

        return resultMapper.toResult(saved);
    }
}
