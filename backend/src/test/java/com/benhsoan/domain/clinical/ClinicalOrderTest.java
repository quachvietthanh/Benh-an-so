package com.benhsoan.domain.clinical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.clinical.enums.ClinicalOrderStatus;
import com.benhsoan.domain.clinical.exception.ClinicalOrderAlreadyCompletedException;
import com.benhsoan.domain.clinical.exception.ClinicalOrderInvalidStatusException;
import com.benhsoan.domain.shared.exception.ValidationException;

class ClinicalOrderTest {

    private final Instant orderedAt = Instant.parse("2026-08-20T02:00:00Z");

    @Test
    void followsOrderLifecycle() {
        ClinicalOrder order = createOrder();

        order.start(orderedAt.plusSeconds(1));
        order.markPartiallyCompleted(orderedAt.plusSeconds(2));
        order.complete(orderedAt.plusSeconds(3));

        assertEquals(ClinicalOrderStatus.COMPLETED, order.getStatus());
    }

    @Test
    void rejectsCompletionBeforeOrderTime() {
        ClinicalOrder order = createOrder();
        order.start(orderedAt.plusSeconds(1));

        assertThrows(ValidationException.class, () -> order.complete(orderedAt.minusSeconds(1)));
    }

    @Test
    void rejectsStartingCompletedOrder() {
        ClinicalOrder order = createOrder();
        order.start(orderedAt);
        order.complete(orderedAt.plusSeconds(1));

        assertThrows(ClinicalOrderAlreadyCompletedException.class,
                () -> order.start(orderedAt.plusSeconds(2)));
    }

    @Test
    void rejectsCompletionWithoutStarting() {
        assertThrows(ClinicalOrderInvalidStatusException.class,
                () -> createOrder().complete(orderedAt.plusSeconds(1)));
    }

    private ClinicalOrder createOrder() {
        return ClinicalOrder.create(
                "ORD-001", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Assessment", orderedAt
        );
    }
}
