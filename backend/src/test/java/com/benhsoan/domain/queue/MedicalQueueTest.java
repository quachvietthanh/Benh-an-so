package com.benhsoan.domain.queue;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.queue.enums.PriorityLevel;
import com.benhsoan.domain.queue.enums.QueueStatus;
import com.benhsoan.domain.queue.exception.InvalidStatusTransitionException;

@DisplayName("MedicalQueue - Domain State Machine Tests")
class MedicalQueueTest {

    private final UUID patientId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final UUID createdBy = UUID.randomUUID();

    private MedicalQueue createWaitingQueue() {
        return MedicalQueue.create(
                patientId, 1, PriorityLevel.REGULAR, "Room 101", createdBy
        );
    }

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("Should create queue with WAITING status")
        void createSuccess() {
            MedicalQueue queue = createWaitingQueue();

            assertNotNull(queue.getId());
            assertEquals(patientId, queue.getPatientId());
            assertEquals(1, queue.getQueueNumber());
            assertEquals(QueueStatus.WAITING, queue.getStatus());
            assertEquals(PriorityLevel.REGULAR, queue.getPriorityLevel());
            assertEquals("Room 101", queue.getRoomNumber());
            assertNull(queue.getDoctorId());
            assertNotNull(queue.getCheckedInAt());
        }

        @Test
        @DisplayName("Should create queue with doctorId when provided")
        void createWithDoctorId() {
            MedicalQueue queue = MedicalQueue.create(
                    patientId, 1, PriorityLevel.REGULAR, "Room 101", doctorId, createdBy
            );

            assertEquals(doctorId, queue.getDoctorId());
        }
    }

    @Nested
    @DisplayName("WAITING → IN_PROGRESS (call)")
    class CallTransition {

        @Test
        @DisplayName("Should transition WAITING → IN_PROGRESS")
        void callSuccess() {
            MedicalQueue queue = createWaitingQueue();
            queue.call(doctorId);

            assertEquals(QueueStatus.IN_PROGRESS, queue.getStatus());
            assertEquals(doctorId, queue.getDoctorId());
            assertNotNull(queue.getCalledAt());
            assertNotNull(queue.getStartedAt());
        }
    }

    @Nested
    @DisplayName("WAITING → SKIPPED (skip)")
    class SkipTransition {

        @Test
        @DisplayName("Should transition WAITING → SKIPPED")
        void skipSuccess() {
            MedicalQueue queue = createWaitingQueue();
            queue.skip();

            assertEquals(QueueStatus.SKIPPED, queue.getStatus());
        }
    }

    @Nested
    @DisplayName("SKIPPED → IN_PROGRESS (resumeFromSkipped)")
    class ResumeFromSkippedTransition {

        @Test
        @DisplayName("Should transition SKIPPED → IN_PROGRESS")
        void resumeFromSkippedSuccess() {
            MedicalQueue queue = createWaitingQueue();
            queue.skip();
            queue.resumeFromSkipped();

            assertEquals(QueueStatus.IN_PROGRESS, queue.getStatus());
            assertNotNull(queue.getCalledAt());
            assertNotNull(queue.getStartedAt());
        }

        @Test
        @DisplayName("Should throw resumeFromSkipped from non-SKIPPED")
        void cannotResumeFromNonSkipped() {
            MedicalQueue queue = createWaitingQueue();
            assertThrows(InvalidStatusTransitionException.class,
                    queue::resumeFromSkipped);
        }
    }

    @Nested
    @DisplayName("IN_PROGRESS transitions")
    class InProgressTransitions {

        @Test
        @DisplayName("Should transition IN_PROGRESS → WAITING_FOR_RESULT")
        void sendToWaitingForResultSuccess() {
            MedicalQueue queue = createWaitingQueue();
            queue.call(doctorId);
            queue.sendToWaitingForResult();

            assertEquals(QueueStatus.WAITING_FOR_RESULT, queue.getStatus());
            assertNotNull(queue.getWaitingForResultAt());
        }

        @Test
        @DisplayName("Should transition IN_PROGRESS → COMPLETED")
        void completeSuccess() {
            MedicalQueue queue = createWaitingQueue();
            queue.call(doctorId);
            queue.complete();

            assertEquals(QueueStatus.COMPLETED, queue.getStatus());
            assertNotNull(queue.getCompletedAt());
        }

        @Test
        @DisplayName("Should transition IN_PROGRESS → CANCELLED")
        void cancelFromInProgress() {
            MedicalQueue queue = createWaitingQueue();
            queue.call(doctorId);
            queue.cancel("Emergency");

            assertEquals(QueueStatus.CANCELLED, queue.getStatus());
            assertEquals("Emergency", queue.getCancelReason());
            assertNotNull(queue.getCancelledAt());
        }
    }

    @Nested
    @DisplayName("WAITING_FOR_RESULT transitions")
    class WaitingForResultTransitions {

        @Test
        @DisplayName("Should transition WAITING_FOR_RESULT → IN_PROGRESS (resume)")
        void resumeSuccess() {
            MedicalQueue queue = createWaitingQueue();
            queue.call(doctorId);
            queue.sendToWaitingForResult();
            queue.resumeFromWaitingForResult();

            assertEquals(QueueStatus.IN_PROGRESS, queue.getStatus());
        }

        @Test
        @DisplayName("Should transition WAITING_FOR_RESULT → COMPLETED")
        void completeSuccess() {
            MedicalQueue queue = createWaitingQueue();
            queue.call(doctorId);
            queue.sendToWaitingForResult();
            queue.complete();

            assertEquals(QueueStatus.COMPLETED, queue.getStatus());
        }

        @Test
        @DisplayName("Should transition WAITING_FOR_RESULT → CANCELLED")
        void cancelSuccess() {
            MedicalQueue queue = createWaitingQueue();
            queue.call(doctorId);
            queue.sendToWaitingForResult();
            queue.cancel("Patient left");

            assertEquals(QueueStatus.CANCELLED, queue.getStatus());
        }
    }

    @Nested
    @DisplayName("SKIPPED → CANCELLED")
    class SkippedToCancelled {

        @Test
        @DisplayName("Should transition SKIPPED → CANCELLED")
        void skippedToCancelled() {
            MedicalQueue queue = createWaitingQueue();
            queue.skip();
            queue.cancel("Doctor skipped");

            assertEquals(QueueStatus.CANCELLED, queue.getStatus());
        }
    }

    @Nested
    @DisplayName("Invalid transitions")
    class InvalidTransitions {

        @Test
        @DisplayName("Should throw from WAITING → COMPLETED")
        void cannotCompleteFromWaiting() {
            MedicalQueue queue = createWaitingQueue();
            assertThrows(InvalidStatusTransitionException.class,
                    queue::complete);
        }

        @Test
        @DisplayName("Should throw from WAITING → WAITING_FOR_RESULT")
        void cannotSendToResultFromWaiting() {
            MedicalQueue queue = createWaitingQueue();
            assertThrows(InvalidStatusTransitionException.class,
                    queue::sendToWaitingForResult);
        }

        @Test
        @DisplayName("Should throw from COMPLETED → CANCELLED")
        void cannotCancelFromCompleted() {
            MedicalQueue queue = createWaitingQueue();
            queue.call(doctorId);
            queue.complete();
            assertThrows(InvalidStatusTransitionException.class,
                    () -> queue.cancel("Too late"));
        }

        @Test
        @DisplayName("Should throw from CANCELLED → any")
        void cannotTransitionFromCancelled() {
            MedicalQueue queue = createWaitingQueue();
            queue.cancel("Cancelled");
            assertThrows(InvalidStatusTransitionException.class,
                    queue::complete);
            assertThrows(InvalidStatusTransitionException.class,
                    queue::sendToWaitingForResult);
            assertThrows(InvalidStatusTransitionException.class,
                    queue::skip);
        }

        @Test
        @DisplayName("Should throw resume from non-WAITING_FOR_RESULT")
        void cannotResumeFromNonWaitingResult() {
            MedicalQueue queue = createWaitingQueue();
            assertThrows(InvalidStatusTransitionException.class,
                    queue::resumeFromWaitingForResult);
        }

        @Test
        @DisplayName("Should throw call from COMPLETED")
        void cannotCallCompleted() {
            MedicalQueue queue = createWaitingQueue();
            queue.call(doctorId);
            queue.complete();
            assertThrows(InvalidStatusTransitionException.class,
                    () -> queue.call(doctorId));
        }
    }

    @Nested
    @DisplayName("Restore")
    class Restore {

        @Test
        @DisplayName("Should restore from persisted data")
        void restoreSuccess() {
            UUID id = UUID.randomUUID();
            MedicalQueue restored = MedicalQueue.restore(
                    id, patientId, doctorId, "Room 101",
                    1, QueueStatus.IN_PROGRESS, PriorityLevel.EMERGENCY,
                    "Some notes", null, null, null,
                    null, null, null, null,
                    createdBy, Instant.now(), Instant.now()
            );

            assertEquals(id, restored.getId());
            assertEquals(patientId, restored.getPatientId());
            assertEquals(QueueStatus.IN_PROGRESS, restored.getStatus());
            assertEquals(PriorityLevel.EMERGENCY, restored.getPriorityLevel());
            assertEquals("Some notes", restored.getNotes());
        }
    }
}
