package com.benhsoan.domain.visit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.domain.visit.exception.VisitAlreadyCancelledException;
import com.benhsoan.domain.visit.exception.VisitAlreadyCompletedException;
import com.benhsoan.domain.visit.exception.VisitInvalidStatusException;

class VisitTest {

    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");

    private Visit visit() {
        return Visit.create("V001", UUID.randomUUID(), UUID.randomUUID(), null, null, VisitType.WALK_IN, now, "Reason", null, UUID.randomUUID());
    }

    private Visit inProgress() {
        Visit v = visit();
        v.start(now);
        return v;
    }

    @Test
    void followsLifecycle() {
        Visit v = visit();
        v.start(now);
        v.waitForResult(now.plusSeconds(1));
        v.resume(now.plusSeconds(2));
        v.complete(now.plusSeconds(3));
        assertTrue(v.isCompleted());
    }

    @Test
    void rejectsCompletionBeforeStart() {
        Visit v = visit();
        v.start(now);
        assertThrows(ValidationException.class, () -> v.complete(now.minusSeconds(1)));
    }

    @Test
    void rejectsRegistrationChangeAfterStart() {
        Visit v = visit();
        v.start(now);
        assertThrows(VisitInvalidStatusException.class, () -> v.updateRegistrationInformation(UUID.randomUUID(), null, null, VisitType.FOLLOW_UP, now, "R", null, now));
    }

    @Test
    void revertsInProgressToWaiting() {
        Visit v = visit();
        v.start(now);
        v.revertToWaiting(now.plusSeconds(10));
        assertEquals(VisitStatus.WAITING, v.getStatus());
        assertNull(v.getStartedAt());
        assertEquals(now.plusSeconds(10), v.getUpdatedAt());
    }

    @Test
    void rejectsRevertToWaitingWhenNotInProgress() {
        Visit v = visit();
        assertThrows(VisitInvalidStatusException.class, () -> v.revertToWaiting(now));
    }

    // ---- earlyEnd ----

    @Test
    void earlyEndsInProgressVisitAndPersistsReason() {
        Visit v = inProgress();
        v.earlyEnd("Bệnh nhân bỏ về", now.plusSeconds(60));

        assertEquals(VisitStatus.EARLY_ENDED, v.getStatus());
        assertEquals("Bệnh nhân bỏ về", v.getCloseReason());
        assertEquals(now.plusSeconds(60), v.getClosedAt());
        assertEquals(now.plusSeconds(60), v.getUpdatedAt());
        assertTrue(v.isEarlyEnded());
    }

    @Test
    void earlyEndTrimsReason() {
        Visit v = inProgress();
        v.earlyEnd("  Bệnh nhân chuyển tuyến  ", now.plusSeconds(60));
        assertEquals("Bệnh nhân chuyển tuyến", v.getCloseReason());
    }

    @Test
    void earlyEndRejectsBlankReason() {
        Visit v = inProgress();
        assertThrows(ValidationException.class, () -> v.earlyEnd("   ", now.plusSeconds(60)));
        assertThrows(ValidationException.class, () -> v.earlyEnd(null, now.plusSeconds(60)));
    }

    @Test
    void earlyEndRejectsReasonExceeding500Characters() {
        Visit v = inProgress();
        String longReason = "x".repeat(501);
        assertThrows(ValidationException.class, () -> v.earlyEnd(longReason, now.plusSeconds(60)));
    }

    @Test
    void earlyEndRejectsWaiting() {
        Visit v = visit();
        assertThrows(VisitInvalidStatusException.class, () -> v.earlyEnd("reason", now));
    }

    @Test
    void earlyEndRejectsWaitingForResult() {
        Visit v = visit();
        v.start(now);
        v.waitForResult(now.plusSeconds(1));
        assertThrows(VisitInvalidStatusException.class, () -> v.earlyEnd("reason", now.plusSeconds(2)));
    }

    @Test
    void earlyEndRejectsCompleted() {
        Visit v = visit();
        v.start(now);
        v.complete(now.plusSeconds(1));
        assertThrows(VisitAlreadyCompletedException.class, () -> v.earlyEnd("reason", now.plusSeconds(2)));
    }

    @Test
    void earlyEndRejectsCancelled() {
        Visit v = visit();
        v.cancel(now);
        assertThrows(VisitAlreadyCancelledException.class, () -> v.earlyEnd("reason", now.plusSeconds(1)));
    }

    // ---- cancel(reason) ----

    @Test
    void cancelsInProgressVisitWithReason() {
        Visit v = inProgress();
        v.cancel("Nhập nhầm lượt khám", now.plusSeconds(60));

        assertEquals(VisitStatus.CANCELLED, v.getStatus());
        assertEquals("Nhập nhầm lượt khám", v.getCloseReason());
        assertEquals(now.plusSeconds(60), v.getClosedAt());
        assertEquals(now.plusSeconds(60), v.getUpdatedAt());
    }

    @Test
    void cancelWithReasonRejectsBlankReason() {
        Visit v = inProgress();
        assertThrows(ValidationException.class, () -> v.cancel("   ", now.plusSeconds(60)));
    }

    @Test
    void cancelWithReasonRejectsReasonExceeding500Characters() {
        Visit v = inProgress();
        assertThrows(ValidationException.class, () -> v.cancel("x".repeat(501), now.plusSeconds(60)));
    }

    @Test
    void cancelWithReasonRejectsNonInProgressStates() {
        Visit waiting = visit();
        assertThrows(VisitInvalidStatusException.class, () -> waiting.cancel("reason", now));

        Visit waitingForResult = visit();
        waitingForResult.start(now);
        waitingForResult.waitForResult(now.plusSeconds(1));
        assertThrows(VisitInvalidStatusException.class, () -> waitingForResult.cancel("reason", now.plusSeconds(2)));

        Visit completed = visit();
        completed.start(now);
        completed.complete(now.plusSeconds(1));
        assertThrows(VisitAlreadyCompletedException.class, () -> completed.cancel("reason", now.plusSeconds(2)));

        Visit cancelled = visit();
        cancelled.cancel(now);
        assertThrows(VisitAlreadyCancelledException.class, () -> cancelled.cancel("reason", now.plusSeconds(1)));

        Visit earlyEnded = visit();
        earlyEnded.start(now);
        earlyEnded.earlyEnd("reason", now.plusSeconds(1));
        assertThrows(VisitInvalidStatusException.class, () -> earlyEnded.cancel("reason", now.plusSeconds(2)));
    }
}
