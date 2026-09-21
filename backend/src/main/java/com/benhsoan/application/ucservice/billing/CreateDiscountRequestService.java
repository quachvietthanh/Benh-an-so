package com.benhsoan.application.ucservice.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
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

    @Override
    public DiscountRequestResult create(CreateDiscountRequestCommand command) {
        if (command == null || command.visitId() == null) {
            throw new ValidationException("Mã lượt khám (visitId) là bắt buộc.");
        }

        ensureAuthorized();

        Visit visit = visitRepository.findById(command.visitId())
                .orElseThrow(() -> new VisitNotFoundException(command.visitId()));

        if (visit.getStatus() == VisitStatus.CANCELLED) {
            throw new ValidationException("Không thể tạo yêu cầu giảm giá cho lượt khám đã hủy.");
        }

        if (paymentRepository.findByVisitId(command.visitId()).isPresent()) {
            throw new ValidationException("Lượt khám đã thanh toán, không thể yêu cầu giảm giá.");
        }

        if (discountRequestRepository.existsByVisitIdAndStatus(command.visitId(), DiscountRequestStatus.PENDING)
                || discountRequestRepository.existsByVisitIdAndStatus(command.visitId(), DiscountRequestStatus.APPROVED)) {
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
                now
        );

        DiscountRequest saved = discountRequestRepository.save(request);

        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.CREATE,
                ResourceType.DISCOUNT_REQUEST,
                saved.getId(),
                """
                {
                "visitId":"%s",
                "discountType":"%s",
                "discountValue":"%s",
                "originalAmount":"%s",
                "discountAmount":"%s",
                "finalAmount":"%s",
                "reason":"%s"
                }
                """.formatted(
                        saved.getVisitId(),
                        saved.getDiscountType(),
                        saved.getDiscountValue(),
                        saved.getOriginalAmount(),
                        saved.getDiscountAmount(),
                        saved.getFinalAmount(),
                        saved.getReason()
                ),
                null,
                now
        ));

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
