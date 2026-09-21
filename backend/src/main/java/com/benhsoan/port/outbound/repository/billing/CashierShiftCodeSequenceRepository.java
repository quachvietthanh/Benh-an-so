package com.benhsoan.port.outbound.repository.billing;

public interface CashierShiftCodeSequenceRepository {

    long reserveNextValue(String prefix);
}
