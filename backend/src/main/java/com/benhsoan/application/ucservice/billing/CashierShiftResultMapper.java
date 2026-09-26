package com.benhsoan.application.ucservice.billing;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.billing.CashierShift;
import com.benhsoan.domain.auth.User;
import com.benhsoan.port.dto.result.CashierShiftResult;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CashierShiftResultMapper {

    private final UserRepository userRepository;

    public CashierShiftResult toResult(CashierShift shift) {
        if (shift == null) {
            return null;
        }

        String cashierName = resolveUserName(shift.getCashierId());
        String confirmedByName = shift.getConfirmedBy() != null
                ? resolveUserName(shift.getConfirmedBy())
                : null;

        return new CashierShiftResult(
                shift.getId(),
                shift.getShiftCode(),
                shift.getCashierId(),
                cashierName,
                shift.getStartTime(),
                shift.getEndTime(),
                shift.getTotalTransactions(),
                shift.getTotalSystemAmount(),
                shift.getSystemCashAmount(),
                shift.getSystemTransferAmount(),
                shift.getSystemCardAmount(),
                shift.getSystemOtherAmount(),
                shift.getActualCashAmount(),
                shift.getDifferenceAmount(),
                shift.getStatus(),
                shift.getNotes(),
                shift.getConfirmedBy(),
                confirmedByName,
                shift.getConfirmedAt(),
                shift.getConfirmationNotes(),
                shift.getCreatedAt()
        );
    }

    private String resolveUserName(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map(User::getFullName)
                .orElse(userId.toString());
    }
}
