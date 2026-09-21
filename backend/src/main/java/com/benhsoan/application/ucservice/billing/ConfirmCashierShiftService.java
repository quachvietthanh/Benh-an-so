package com.benhsoan.application.ucservice.billing;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.billing.CashierShift;
import com.benhsoan.domain.billing.exception.CashierShiftNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.billing.ConfirmCashierShiftCommand;
import com.benhsoan.port.dto.result.CashierShiftResult;
import com.benhsoan.port.inbound.billing.ConfirmCashierShiftUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.billing.CashierShiftRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ConfirmCashierShiftService implements ConfirmCashierShiftUseCase {

    private final CashierShiftRepository cashierShiftRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final AuditLogRepository auditLogRepository;
    private final CashierShiftResultMapper resultMapper;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    public CashierShiftResult confirm(ConfirmCashierShiftCommand command) {
        validateCommand(command);
        ensureAuthorized();

        UUID managerId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();

        CashierShift shift = cashierShiftRepository.findByIdForUpdate(command.shiftId())
                .orElseThrow(() -> new CashierShiftNotFoundException(command.shiftId()));

        shift.confirm(managerId, now, command.confirmationNotes());
        CashierShift saved = cashierShiftRepository.save(shift);

        String detailJson;
        try {
            detailJson = objectMapper.writeValueAsString(java.util.Map.of(
                    "shiftCode", saved.getShiftCode(),
                    "status", saved.getStatus().name(),
                    "confirmedBy", managerId.toString(),
                    "confirmedAt", now.toString(),
                    "confirmationNotes", saved.getConfirmationNotes() != null ? saved.getConfirmationNotes() : ""
            ));
        } catch (Exception e) {
            detailJson = "{}";
        }

        auditLogRepository.save(AuditLog.create(
                managerId,
                ActionType.UPDATE,
                ResourceType.CASHIER_SHIFT,
                saved.getId(),
                detailJson,
                null,
                now
        ));

        return resultMapper.toResult(saved);
    }

    private void ensureAuthorized() {
        if (!currentUserPort.hasRole("MANAGER") && !currentUserPort.hasRole("ADMIN")) {
            throw new AccessDeniedException("Chỉ Quản lý phòng khám mới có quyền xác nhận phiếu chốt ca.");
        }
    }

    private void validateCommand(ConfirmCashierShiftCommand command) {
        if (command == null || command.shiftId() == null) {
            throw new ValidationException("Mã định danh phiếu chốt ca không được để trống.");
        }
    }
}
