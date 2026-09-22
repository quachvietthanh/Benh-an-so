package com.benhsoan.infrastructure.security.generator;

import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.generator.CashierShiftCodeGenerator;
import com.benhsoan.port.outbound.repository.billing.CashierShiftCodeSequenceRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DatabaseCashierShiftCodeGenerator implements CashierShiftCodeGenerator {

    private static final String PREFIX = "CS";

    private final CashierShiftCodeSequenceRepository sequenceRepository;

    @Override
    public String generate() {
        return PREFIX + String.format(
                "%06d",
                sequenceRepository.reserveNextValue(PREFIX)
        );
    }
}
