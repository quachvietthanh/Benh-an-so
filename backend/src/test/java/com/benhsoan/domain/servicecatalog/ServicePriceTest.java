package com.benhsoan.domain.servicecatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.shared.exception.ValidationException;

class ServicePriceTest {

    @Test
    void createAcceptsZeroPrice() {
        ServicePrice price = ServicePrice.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.ZERO,
                LocalDate.of(2026, 1, 1),
                Instant.parse("2026-01-01T00:00:00Z"),
                UUID.randomUUID()
        );

        assertEquals(0, price.getPrice().compareTo(BigDecimal.ZERO));
    }

    @Test
    void createRejectsNegativePrice() {
        assertThrows(
                ValidationException.class,
                () -> ServicePrice.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("-0.01"),
                        LocalDate.of(2026, 1, 1),
                        Instant.parse("2026-01-01T00:00:00Z"),
                        UUID.randomUUID()
                )
        );
    }
}
