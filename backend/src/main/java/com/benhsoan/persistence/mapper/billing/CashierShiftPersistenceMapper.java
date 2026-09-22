package com.benhsoan.persistence.mapper.billing;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.billing.CashierShift;
import com.benhsoan.persistence.entity.billing.CashierShiftEntity;

@Component
public class CashierShiftPersistenceMapper {

    public CashierShift toDomain(CashierShiftEntity entity) {
        if (entity == null) {
            return null;
        }
        return CashierShift.restore(
                entity.getId(),
                entity.getShiftCode(),
                entity.getCashierId(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getTotalTransactions(),
                entity.getTotalSystemAmount(),
                entity.getSystemCashAmount(),
                entity.getSystemTransferAmount(),
                entity.getSystemCardAmount(),
                entity.getSystemOtherAmount(),
                entity.getActualCashAmount(),
                entity.getDifferenceAmount(),
                entity.getStatus(),
                entity.getNotes(),
                entity.getConfirmedBy(),
                entity.getConfirmedAt(),
                entity.getConfirmationNotes(),
                entity.getCreatedAt()
        );
    }

    public CashierShiftEntity toEntity(CashierShift domain) {
        if (domain == null) {
            return null;
        }
        return CashierShiftEntity.builder()
                .id(domain.getId())
                .shiftCode(domain.getShiftCode())
                .cashierId(domain.getCashierId())
                .startTime(domain.getStartTime())
                .endTime(domain.getEndTime())
                .totalTransactions(domain.getTotalTransactions())
                .totalSystemAmount(domain.getTotalSystemAmount())
                .systemCashAmount(domain.getSystemCashAmount())
                .systemTransferAmount(domain.getSystemTransferAmount())
                .systemCardAmount(domain.getSystemCardAmount())
                .systemOtherAmount(domain.getSystemOtherAmount())
                .actualCashAmount(domain.getActualCashAmount())
                .differenceAmount(domain.getDifferenceAmount())
                .status(domain.getStatus())
                .notes(domain.getNotes())
                .confirmedBy(domain.getConfirmedBy())
                .confirmedAt(domain.getConfirmedAt())
                .confirmationNotes(domain.getConfirmationNotes())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
