package com.benhsoan.domain.clinical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus;
import com.benhsoan.domain.clinical.exception.ClinicalOrderItemInvalidStatusException;

class ClinicalOrderItemTest {

    @Test
    void completesPendingItem() {
        ClinicalOrderItem item = createItem();

        item.complete(Instant.parse("2026-08-20T02:00:00Z"));

        assertEquals(ClinicalOrderItemStatus.COMPLETED, item.getStatus());
    }

    @Test
    void rejectsCancellingCompletedItem() {
        ClinicalOrderItem item = createItem();
        item.complete(Instant.parse("2026-08-20T02:00:00Z"));

        assertThrows(ClinicalOrderItemInvalidStatusException.class,
                () -> item.cancel(Instant.parse("2026-08-20T02:01:00Z")));
    }

    private ClinicalOrderItem createItem() {
        return ClinicalOrderItem.create(
                UUID.randomUUID(), UUID.randomUUID(), "LAB-001", "Blood test", null
        );
    }
}
