package com.benhsoan.application.ucservice.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.billing.CashierShift;
import com.benhsoan.domain.billing.Payment;
import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.domain.billing.exception.NoUnsettledPaymentsException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.billing.CloseCashierShiftCommand;
import com.benhsoan.port.dto.result.CashierShiftResult;
import com.benhsoan.port.inbound.billing.CloseCashierShiftUseCase;
import com.benhsoan.port.outbound.generator.CashierShiftCodeGenerator;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.billing.CashierShiftRepository;
import com.benhsoan.port.outbound.repository.billing.PaymentRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CloseCashierShiftService implements CloseCashierShiftUseCase {

    private static final List<PaymentStatus> ELIGIBLE_STATUSES = List.of(
            PaymentStatus.RECORDED,
            PaymentStatus.SUCCESS,
            PaymentStatus.REFUNDED
    );

    private final PaymentRepository paymentRepository;
    private final CashierShiftRepository cashierShiftRepository;
    private final CashierShiftCodeGenerator shiftCodeGenerator;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final AuditLogRepository auditLogRepository;
    private final CashierShiftResultMapper resultMapper;
    private final CashierShiftAuthorizationAuditService authorizationAuditService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    public CashierShiftResult close(CloseCashierShiftCommand command) {
        validateCommand(command);

        UUID cashierId = currentUserPort.getCurrentUserId();
        ensureAuthorized(cashierId);

        Instant now = clockPort.now();

        List<Payment> unsettled = paymentRepository
                .findUnsettledByCashierForUpdate(cashierId, ELIGIBLE_STATUSES);

        if (unsettled.isEmpty()) {
            throw new NoUnsettledPaymentsException();
        }

        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal transfer = BigDecimal.ZERO;
        BigDecimal card = BigDecimal.ZERO;
        BigDecimal other = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        for (Payment payment : unsettled) {
            BigDecimal amount = payment.getAmountPaid();
            boolean isRefunded = payment.getStatus() == PaymentStatus.REFUNDED;
            BigDecimal effectiveAmount = isRefunded ? amount.negate() : amount;

            total = total.add(effectiveAmount);
            if (payment.getPaymentMethod() == PaymentMethod.CASH) {
                cash = cash.add(effectiveAmount);
            } else if (payment.getPaymentMethod() == PaymentMethod.BANK_TRANSFER) {
                transfer = transfer.add(effectiveAmount);
            } else if (payment.getPaymentMethod() == PaymentMethod.CARD) {
                card = card.add(effectiveAmount);
            } else {
                other = other.add(effectiveAmount);
            }
        }

        Instant startTime = unsettled.get(0).getPaidAt();
        Instant endTime = now;
        UUID shiftId = UUID.randomUUID();
        String shiftCode = shiftCodeGenerator.generate();

        CashierShift shift = CashierShift.create(
                shiftId,
                shiftCode,
                cashierId,
                startTime,
                endTime,
                unsettled.size(),
                total,
                cash,
                transfer,
                card,
                other,
                command.actualCashAmount(),
                command.notes(),
                now
        );

        CashierShift savedShift = cashierShiftRepository.save(shift);

        for (Payment payment : unsettled) {
            payment.assignToShift(savedShift.getId());
        }
        paymentRepository.saveAll(unsettled);

        String detailJson;
        try {
            detailJson = objectMapper.writeValueAsString(java.util.Map.of(
                    "shiftCode", savedShift.getShiftCode(),
                    "totalTransactions", savedShift.getTotalTransactions(),
                    "totalSystemAmount", savedShift.getTotalSystemAmount().toString(),
                    "systemCashAmount", savedShift.getSystemCashAmount().toString(),
                    "actualCashAmount", savedShift.getActualCashAmount().toString(),
                    "differenceAmount", savedShift.getDifferenceAmount().toString(),
                    "status", savedShift.getStatus().name(),
                    "notes", savedShift.getNotes() != null ? savedShift.getNotes() : ""
            ));
        } catch (Exception e) {
            detailJson = "{}";
        }

        auditLogRepository.save(AuditLog.create(
                cashierId,
                ActionType.CREATE,
                ResourceType.CASHIER_SHIFT,
                savedShift.getId(),
                detailJson,
                null,
                now
        ));

        return resultMapper.toResult(savedShift);
    }

    private void ensureAuthorized(UUID actorId) {
        if (!currentUserPort.hasPermission("CASHIER_SHIFT_CREATE")
                && !currentUserPort.hasRole("RECEPTIONIST")
                && !currentUserPort.hasRole("ADMIN")) {
            authorizationAuditService.recordCloseAccessDenied(
                    actorId,
                    "Chỉ Thu ngân hoặc Quản trị viên mới có quyền chốt ca."
            );
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Thu ngân hoặc Quản trị viên mới có quyền chốt ca.");
        }
    }

    private void validateCommand(CloseCashierShiftCommand command) {
        if (command == null) {
            throw new ValidationException("Yêu cầu chốt ca không được để trống.");
        }
        if (command.actualCashAmount() == null) {
            throw new ValidationException("Số tiền mặt thực tế không được để trống.");
        }
        if (command.actualCashAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Số tiền mặt thực tế không được âm.");
        }
    }
}
