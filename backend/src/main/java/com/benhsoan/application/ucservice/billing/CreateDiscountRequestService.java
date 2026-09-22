package com.benhsoan.application.ucservice.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
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
import com.benhsoan.domain.billing.enums.DiscountRequestStatus;
import com.benhsoan.domain.billing.exception.DiscountAlreadyExistsException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.billing.CreateDiscountRequestCommand;
import com.benhsoan.port.dto.result.DiscountRequestResult;
import com.benhsoan.port.inbound.billing.CreateDiscountRequestUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository;
import com.benhsoan.port.outbound.repository.billing.PaymentRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class CreateDiscountRequestService implements CreateDiscountRequestUseCase {

    private final VisitRepository visitRepository;
    private final PaymentRepository paymentRepository;
    private final DiscountRequestRepository discountRequestRepository;
    private final ClinicalServiceFeeCalculator clinicalServiceFeeCalculator;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final AuditLogRepository auditLogRepository;
    private final DiscountRequestResultMapper resultMapper;
    private final ObjectMapper objectMapper;

    @Autowired
    public CreateDiscountRequestService(
            VisitRepository visitRepository,
            PaymentRepository paymentRepository,
            DiscountRequestRepository discountRequestRepository,
            ClinicalServiceFeeCalculator clinicalServiceFeeCalculator,
            CurrentUserPort currentUserPort,
            ClockPort clockPort,
            AuditLogRepository auditLogRepository,
            DiscountRequestResultMapper resultMapper,
            ObjectMapper objectMapper) {
        this.visitRepository = visitRepository;
        this.paymentRepository = paymentRepository;
        this.discountRequestRepository = discountRequestRepository;
        this.clinicalServiceFeeCalculator = clinicalServiceFeeCalculator;
        this.currentUserPort = currentUserPort;
        this.clockPort = clockPort;
        this.auditLogRepository = auditLogRepository;
        this.resultMapper = resultMapper;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public CreateDiscountRequestService(
            VisitRepository visitRepository,
            PaymentRepository paymentRepository,
            DiscountRequestRepository discountRequestRepository,
            ClinicalServiceFeeCalculator clinicalServiceFeeCalculator,
            CurrentUserPort currentUserPort,
            ClockPort clockPort,
            AuditLogRepository auditLogRepository,
            DiscountRequestResultMapper resultMapper) {
        this(
                visitRepository,
                paymentRepository,
                discountRequestRepository,
                clinicalServiceFeeCalculator,
                currentUserPort,
                clockPort,
                auditLogRepository,
                resultMapper,
                new ObjectMapper());
    }

    @Override
    public DiscountRequestResult create(CreateDiscountRequestCommand command) {
        if (command == null || command.visitId() == null) {
            throw new ValidationException("Mã lượt khám (visitId) là bắt buộc.");
        }

        ensureAuthorized();

        Visit visit = visitRepository.findByIdForUpdate(command.visitId())
                .or(() -> visitRepository.findById(command.visitId()))
                .orElseThrow(() -> new VisitNotFoundException(command.visitId()));

        if (visit.getStatus() == VisitStatus.CANCELLED) {
            throw new ValidationException("Không thể tạo yêu cầu giảm giá cho lượt khám đã hủy.");
        }

        if (paymentRepository.findByVisitId(command.visitId()).isPresent()) {
            throw new ValidationException("Lượt khám đã thanh toán, không thể yêu cầu giảm giá.");
        }

        if (discountRequestRepository.existsByVisitIdAndStatus(command.visitId(), DiscountRequestStatus.PENDING)
                || discountRequestRepository.existsByVisitIdAndStatus(command.visitId(),
                        DiscountRequestStatus.APPROVED)) {
            throw new DiscountAlreadyExistsException(command.visitId());
        }

        UUID actorId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();

        BigDecimal originalAmount = resolveOriginalAmount(command, now);

        DiscountRequest request = DiscountRequest.create(
                UUID.randomUUID(),
                command.visitId(),
                command.discountType(),
                command.discountValue(),
                originalAmount,
                command.reason(),
                actorId,
                now);

        DiscountRequest saved = discountRequestRepository.save(request);

        Map<String, Object> auditPayload = new LinkedHashMap<>();
        auditPayload.put("visitId", saved.getVisitId() != null ? saved.getVisitId().toString() : null);
        auditPayload.put("discountType", saved.getDiscountType() != null ? saved.getDiscountType().name() : null);
        auditPayload.put("discountValue",
                saved.getDiscountValue() != null ? saved.getDiscountValue().toString() : null);
        auditPayload.put("originalAmount",
                saved.getOriginalAmount() != null ? saved.getOriginalAmount().toString() : null);
        auditPayload.put("discountAmount",
                saved.getDiscountAmount() != null ? saved.getDiscountAmount().toString() : null);
        auditPayload.put("finalAmount", saved.getFinalAmount() != null ? saved.getFinalAmount().toString() : null);
        auditPayload.put("reason", saved.getReason());

        String auditDetailsJson;
        try {
            auditDetailsJson = objectMapper.writeValueAsString(auditPayload);
        } catch (JsonProcessingException e) {
            auditDetailsJson = "{}";
        }

        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.CREATE,
                ResourceType.DISCOUNT_REQUEST,
                saved.getId(),
                auditDetailsJson,
                null,
                now));

        return resultMapper.toResult(saved);
    }

    private void ensureAuthorized() {
        if (!currentUserPort.hasPermission("INVOICE_CREATE")
                && !currentUserPort.hasRole("ADMIN")
                && !currentUserPort.hasRole("RECEPTIONIST")
                && !currentUserPort.hasRole("MANAGER")) {
            throw new AccessDeniedException("Chỉ nhân viên thu ngân hoặc quản lý mới có quyền tạo đề nghị giảm giá.");
        }
    }

    private BigDecimal resolveOriginalAmount(CreateDiscountRequestCommand command, Instant now) {
        if (command.originalAmount() != null && command.originalAmount().compareTo(BigDecimal.ZERO) > 0) {
            return command.originalAmount();
        }

        List<ClinicalServiceCharge> serviceCharges = clinicalServiceFeeCalculator
                .calculate(command.visitId(), now);
        BigDecimal clinicalTotal = clinicalServiceFeeCalculator.total(serviceCharges);

        if (clinicalTotal.compareTo(BigDecimal.ZERO) > 0) {
            return clinicalTotal;
        }

        throw new ValidationException("Tổng tiền gốc ban đầu phải lớn hơn 0.");
    }
}
