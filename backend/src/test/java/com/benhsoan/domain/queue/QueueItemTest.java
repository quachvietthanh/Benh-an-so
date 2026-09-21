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

    @Test
    void callIncrementsCallCount() {
        Instant checkedInAt = Instant.parse("2026-07-31T01:00:00Z");
        QueueItem item = newWaitingItem(checkedInAt);
        assertEquals(0, item.getCallCount());

        item.call(checkedInAt.plusSeconds(30));
        assertEquals(1, item.getCallCount());
        assertEquals(QueueItemStatus.IN_PROGRESS, item.getStatus());

        item.skip("Absent", checkedInAt.plusSeconds(60));
        assertEquals(QueueItemStatus.SKIPPED, item.getStatus());

        item.reQueue(checkedInAt.plusSeconds(90));
        assertEquals(QueueItemStatus.WAITING, item.getStatus());
        assertEquals(1, item.getCallCount());

        item.call(checkedInAt.plusSeconds(120));
        assertEquals(2, item.getCallCount());
    }

    @Test
    void reQueuesSkippedItem() {
        Instant checkedInAt = Instant.parse("2026-07-31T01:00:00Z");
        QueueItem item = newWaitingItem(checkedInAt);
        item.call(checkedInAt.plusSeconds(30));
        item.skip("Patient absent", checkedInAt.plusSeconds(60));

        Instant reQueuedAt = checkedInAt.plusSeconds(90);
        item.reQueue(reQueuedAt);

        assertEquals(QueueItemStatus.WAITING, item.getStatus());
        assertEquals(reQueuedAt, item.getUpdatedAt());
    }

    @ParameterizedTest
    @MethodSource("nonSkippedItems")
    void rejectsReQueueFromStatusesOtherThanSkipped(QueueItem item) {
        assertThrows(QueueItemInvalidStatusException.class,
                () -> item.reQueue(Instant.now()));
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

    private static Stream<QueueItem> nonSkippedItems() {
        Instant checkedInAt = Instant.parse("2026-07-31T01:00:00Z");
        QueueItem waiting = newWaitingItem(checkedInAt);

        QueueItem inProgress = newWaitingItem(checkedInAt);
        inProgress.call(checkedInAt.plusSeconds(30));

        QueueItem waitingForResult = newWaitingItem(checkedInAt);
        waitingForResult.call(checkedInAt.plusSeconds(30));
        waitingForResult.waitForResult(checkedInAt.plusSeconds(60));

        QueueItem completed = newWaitingItem(checkedInAt);
        completed.call(checkedInAt.plusSeconds(30));
        completed.complete(checkedInAt.plusSeconds(60));

        QueueItem cancelled = newWaitingItem(checkedInAt);
        cancelled.cancel("Cancelled", checkedInAt.plusSeconds(60));

        return Stream.of(waiting, inProgress, waitingForResult, completed, cancelled);
    }

    @Test
    void prioritizeWaitingItemSuccessfully() {
        Instant checkedInAt = Instant.parse("2026-07-31T01:00:00Z");
        QueueItem item = newWaitingItem(checkedInAt);
        UUID actorId = UUID.randomUUID();
        Instant prioritizedAt = checkedInAt.plusSeconds(30);

        item.prioritize(com.benhsoan.domain.queue.enums.QueuePriority.EMERGENCY, "Ca cap cuu - Sot cao co giat", actorId, prioritizedAt);

        assertEquals(com.benhsoan.domain.queue.enums.QueuePriority.EMERGENCY, item.getPriority());
        assertEquals("Ca cap cuu - Sot cao co giat", item.getPriorityReason());
        assertEquals(actorId, item.getPrioritizedBy());
        assertEquals(prioritizedAt, item.getPrioritizedAt());
        assertEquals(prioritizedAt, item.getUpdatedAt());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @org.junit.jupiter.params.provider.ValueSource(strings = {"   ", "\t", "\n"})
    void rejectsPrioritizeWithNullOrBlankReason(String reason) {
        Instant checkedInAt = Instant.parse("2026-07-31T01:00:00Z");
        QueueItem item = newWaitingItem(checkedInAt);

        assertThrows(IllegalArgumentException.class, () ->
                item.prioritize(com.benhsoan.domain.queue.enums.QueuePriority.EMERGENCY, reason, UUID.randomUUID(), checkedInAt.plusSeconds(10)));
    }

    @Test
    void rejectsPrioritizeWhenReasonExceeds500Characters() {
        Instant checkedInAt = Instant.parse("2026-07-31T01:00:00Z");
        QueueItem item = newWaitingItem(checkedInAt);
        String longReason = "a".repeat(501);

        var ex = assertThrows(IllegalArgumentException.class, () ->
                item.prioritize(com.benhsoan.domain.queue.enums.QueuePriority.EMERGENCY, longReason, UUID.randomUUID(), checkedInAt.plusSeconds(10)));
        assertEquals("Priority reason cannot exceed 500 characters.", ex.getMessage());
    }

    @Test
    void acceptsPrioritizeWhenReasonIsExactly500Characters() {
        Instant checkedInAt = Instant.parse("2026-07-31T01:00:00Z");
        QueueItem item = newWaitingItem(checkedInAt);
        String exactReason = "a".repeat(500);

        item.prioritize(com.benhsoan.domain.queue.enums.QueuePriority.EMERGENCY, exactReason, UUID.randomUUID(), checkedInAt.plusSeconds(10));
        assertEquals(exactReason, item.getPriorityReason());
    }

    @Test
    void rejectsPrioritizeWithInvalidPriorityLevel() {
        Instant checkedInAt = Instant.parse("2026-07-31T01:00:00Z");
        QueueItem item = newWaitingItem(checkedInAt);

        assertThrows(IllegalArgumentException.class, () ->
                item.prioritize(null, "Reason", UUID.randomUUID(), checkedInAt.plusSeconds(10)));
        assertThrows(IllegalArgumentException.class, () ->
                item.prioritize(com.benhsoan.domain.queue.enums.QueuePriority.NORMAL, "Reason", UUID.randomUUID(), checkedInAt.plusSeconds(10)));
    }

    @ParameterizedTest
    @MethodSource("nonWaitingItems")
    void rejectsPrioritizeFromStatusesOtherThanWaiting(QueueItem item) {
        assertThrows(QueueItemInvalidStatusException.class, () ->
                item.prioritize(com.benhsoan.domain.queue.enums.QueuePriority.EMERGENCY, "Emergency", UUID.randomUUID(), Instant.now()));
    }

    private static Stream<QueueItem> nonWaitingItems() {
        Instant checkedInAt = Instant.parse("2026-07-31T01:00:00Z");

        QueueItem inProgress = newWaitingItem(checkedInAt);
        inProgress.call(checkedInAt.plusSeconds(30));

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
        skipped.skip("Absent", checkedInAt.plusSeconds(60));

        return Stream.of(inProgress, waitingForResult, completed, cancelled, skipped);
    }

    private static QueueItem newWaitingItem(Instant checkedInAt) {
        return QueueItem.create(UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                QueueItemSourceType.WALK_IN, 1, LocalDate.of(2026, 7, 31), UUID.randomUUID(), checkedInAt);
    }
}
