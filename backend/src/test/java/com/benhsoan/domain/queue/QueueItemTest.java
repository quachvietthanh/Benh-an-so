package com.benhsoan.domain.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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

    @Test
    void skipsInProgressItem() {
        Instant checkedInAt = Instant.parse("2026-07-31T01:00:00Z");
        QueueItem item = newWaitingItem(checkedInAt);
        Instant skippedAt = checkedInAt.plusSeconds(120);

        item.call(checkedInAt.plusSeconds(60));
        item.skip("Patient absent when called", skippedAt);

        assertEquals(QueueItemStatus.SKIPPED, item.getStatus());
        assertEquals("Patient absent when called", item.getSkipReason());
        assertEquals(skippedAt, item.getSkippedAt());
        assertEquals(skippedAt, item.getUpdatedAt());
    }

    @ParameterizedTest
    @MethodSource("nonInProgressItems")
    void rejectsSkipFromStatusesOtherThanInProgress(QueueItem item) {
        assertThrows(QueueItemInvalidStatusException.class,
                () -> item.skip("Patient absent when called", item.getCheckedInAt().plusSeconds(300)));
    }

    private static Stream<QueueItem> nonInProgressItems() {
        Instant checkedInAt = Instant.parse("2026-07-31T01:00:00Z");
        QueueItem waiting = newWaitingItem(checkedInAt);

        QueueItem waitingForResult = newWaitingItem(checkedInAt);
        waitingForResult.call(checkedInAt.plusSeconds(30));
        waitingForResult.waitForResult(checkedInAt.plusSeconds(60));

        QueueItem completed = newWaitingItem(checkedInAt);
        completed.call(checkedInAt.plusSeconds(30));
        completed.complete(checkedInAt.plusSeconds(60));

        QueueItem cancelled = newWaitingItem(checkedInAt);
        cancelled.cancel("Cancelled", checkedInAt.plusSeconds(60));

        QueueItem skipped = newWaitingItem(checkedInAt);
        skipped.call(checkedInAt.plusSeconds(30));
        skipped.skip("Patient absent when called", checkedInAt.plusSeconds(60));

        return Stream.of(waiting, waitingForResult, completed, cancelled, skipped);
    }

    private static QueueItem newWaitingItem(Instant checkedInAt) {
        return QueueItem.create(UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                QueueItemSourceType.WALK_IN, 1, LocalDate.of(2026, 7, 31), UUID.randomUUID(), checkedInAt);
    }
}
