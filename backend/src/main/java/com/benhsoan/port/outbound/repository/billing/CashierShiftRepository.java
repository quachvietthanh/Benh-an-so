package com.benhsoan.port.outbound.repository.billing;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.billing.CashierShift;
import com.benhsoan.domain.billing.enums.CashierShiftStatus;

public interface CashierShiftRepository {

    CashierShift save(CashierShift shift);

    Optional<CashierShift> findById(UUID id);

    Optional<CashierShift> findByIdForUpdate(UUID id);

    Optional<CashierShift> findByShiftCode(String shiftCode);

    Page<CashierShift> search(
            UUID cashierId,
            CashierShiftStatus status,
            Instant from,
            Instant to,
            Pageable pageable
    );
}
