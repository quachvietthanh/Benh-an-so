package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.billing.CloseShiftRequest;
import com.benhsoan.adapter.inbound.rest.request.billing.ConfirmShiftRequest;
import com.benhsoan.adapter.inbound.rest.response.billing.CashierShiftResponse;
import com.benhsoan.adapter.inbound.rest.response.billing.CurrentShiftSummaryResponse;
import com.benhsoan.port.dto.command.billing.CloseCashierShiftCommand;
import com.benhsoan.port.dto.command.billing.ConfirmCashierShiftCommand;
import com.benhsoan.port.dto.result.CashierShiftResult;
import com.benhsoan.port.dto.result.CurrentShiftSummaryResult;

@Component
public class CashierShiftRestMapper {

    public CloseCashierShiftCommand toCommand(CloseShiftRequest request) {
        if (request == null) {
            return null;
        }
        return new CloseCashierShiftCommand(
                request.actualCashAmount(),
                request.notes()
        );
    }

    public ConfirmCashierShiftCommand toCommand(UUID shiftId, ConfirmShiftRequest request) {
        return new ConfirmCashierShiftCommand(
                shiftId,
                request != null ? request.confirmationNotes() : null
        );
    }

    public CurrentShiftSummaryResponse toResponse(CurrentShiftSummaryResult result) {
        if (result == null) {
            return null;
        }
        return new CurrentShiftSummaryResponse(
                result.cashierId(),
                result.cashierName(),
                result.startTime(),
                result.endTime(),
                result.totalTransactions(),
                result.systemCashAmount(),
                result.systemTransferAmount(),
                result.systemCardAmount(),
                result.systemOtherAmount(),
                result.totalSystemAmount(),
                result.unsettledPaymentIds()
        );
    }

    public CashierShiftResponse toResponse(CashierShiftResult result) {
        if (result == null) {
            return null;
        }
        return new CashierShiftResponse(
                result.id(),
                result.shiftCode(),
                result.cashierId(),
                result.cashierName(),
                result.startTime(),
                result.endTime(),
                result.totalTransactions(),
                result.totalSystemAmount(),
                result.systemCashAmount(),
                result.systemTransferAmount(),
                result.systemCardAmount(),
                result.systemOtherAmount(),
                result.actualCashAmount(),
                result.differenceAmount(),
                result.status(),
                result.notes(),
                result.confirmedBy(),
                result.confirmedByName(),
                result.confirmedAt(),
                result.confirmationNotes(),
                result.createdAt()
        );
    }
}
