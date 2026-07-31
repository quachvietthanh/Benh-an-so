package com.benhsoan.domain.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.queue.exception.QueueItemInvalidStatusException;

class QueueItemTest {

    @Test
    void callsAndCompletesWaitingItem() {
        Instant checkedInAt = Instant.parse("2026-07-31T01:00:00Z");
        QueueItem item = QueueItem.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                QueueItemSourceType.APPOINTMENT, 1, LocalDate.of(2026, 7, 31), UUID.randomUUID(), checkedInAt);

        item.call(checkedInAt.plusSeconds(60));
        item.complete(checkedInAt.plusSeconds(120));

        assertEquals(QueueItemStatus.COMPLETED, item.getStatus());
        assertEquals(checkedInAt.plusSeconds(120), item.getCompletedAt());
    }

    @Test
    void rejectsInvalidStatusTransition() {
        Instant checkedInAt = Instant.parse("2026-07-31T01:00:00Z");
        QueueItem item = QueueItem.create(UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                QueueItemSourceType.WALK_IN, 1, LocalDate.of(2026, 7, 31), UUID.randomUUID(), checkedInAt);

        assertThrows(QueueItemInvalidStatusException.class, () -> item.complete(checkedInAt.plusSeconds(60)));
    }
}
