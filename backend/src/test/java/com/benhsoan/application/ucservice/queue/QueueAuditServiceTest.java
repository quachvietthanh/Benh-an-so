package com.benhsoan.application.ucservice.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

class QueueAuditServiceTest {

        @Test
        void recordsSkippedQueueItemWithStandardReason() {
                Instant now = Instant.parse("2026-08-02T02:00:00Z");
                QueueItem item = QueueItem.create(UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                                QueueItemSourceType.WALK_IN, 1, LocalDate.of(2026, 8, 2), UUID.randomUUID(), now);
                item.call(now.plusSeconds(30));
                item.skip("Patient absent when called", now.plusSeconds(60));
                AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
                CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
                when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());
                when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

                new QueueAuditService(auditLogRepository, currentUserPort)
                                .recordSkipped(item, SkipQueueItemService.APPOINTMENT_CANCEL_REASON);

                ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
                verify(auditLogRepository).save(captor.capture());
                assertEquals(ActionType.UPDATE, captor.getValue().getActionType());
                assertTrue(captor.getValue().getDetail().contains("\"action\":\"DEFERRED\""));
                assertTrue(captor.getValue().getDetail().contains("\"queueItemId\":\"" + item.getId() + "\""));
                assertTrue(captor.getValue().getDetail().contains("\"status\":\"SKIPPED\""));
                assertTrue(captor.getValue().getDetail()
                                .contains("\"reason\":\"PATIENT_ABSENT_AFTER_CHECK_IN\""));
                assertTrue(captor.getValue().getDetail().contains("\"callCount\":1"));
        }

        @Test
        void recordsQueueItemStatusUpdateWithCallCount() {
                Instant now = Instant.parse("2026-08-02T02:00:00Z");
                QueueItem item = QueueItem.create(UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                                QueueItemSourceType.WALK_IN, 1, LocalDate.of(2026, 8, 2), UUID.randomUUID(), now);
                item.call(now.plusSeconds(30));
                AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
                CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
                when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());
                when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

                new QueueAuditService(auditLogRepository, currentUserPort)
                                .record(ActionType.UPDATE, item);

                ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
                verify(auditLogRepository).save(captor.capture());
                assertEquals(ActionType.UPDATE, captor.getValue().getActionType());
                assertTrue(captor.getValue().getDetail().contains("\"queueItemId\":\"" + item.getId() + "\""));
                assertTrue(captor.getValue().getDetail().contains("\"status\":\"IN_PROGRESS\""));
                assertTrue(captor.getValue().getDetail().contains("\"callCount\":1"));
        }

        @Test
        void recordsCheckInWithSemanticActionAndInitialStatus() {
                Instant now = Instant.parse("2026-08-02T02:00:00Z");
                QueueItem item = QueueItem.create(UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                                QueueItemSourceType.APPOINTMENT, 3, LocalDate.of(2026, 8, 2), UUID.randomUUID(), now);
                AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
                CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
                UUID actorId = UUID.randomUUID();
                when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

                new QueueAuditService(auditLogRepository, currentUserPort)
                                .recordCheckIn(item, QueueItemSourceType.APPOINTMENT, 3, actorId);

                ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
                verify(auditLogRepository).save(captor.capture());
                assertEquals(ActionType.CREATE, captor.getValue().getActionType());
                assertEquals(actorId, captor.getValue().getUserId());
                assertTrue(captor.getValue().getDetail().contains("\"action\":\"CHECK_IN\""));
                assertTrue(captor.getValue().getDetail().contains("\"status\":\"WAITING\""));
                assertTrue(captor.getValue().getDetail().contains("\"callCount\":0"));
                assertTrue(captor.getValue().getDetail().contains("\"queueNumber\":3"));
                assertTrue(captor.getValue().getDetail().contains("\"sourceType\":\"APPOINTMENT\""));
        }

        @Test
        void recordsCallWithSemanticActionAndCallCount() {
                Instant now = Instant.parse("2026-08-02T02:00:00Z");
                QueueItem item = QueueItem.create(UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                                QueueItemSourceType.WALK_IN, 1, LocalDate.of(2026, 8, 2), UUID.randomUUID(), now);
                item.call(now.plusSeconds(30));
                AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
                CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
                when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());
                when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

                new QueueAuditService(auditLogRepository, currentUserPort).recordCall(item);

                ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
                verify(auditLogRepository).save(captor.capture());
                assertEquals(ActionType.UPDATE, captor.getValue().getActionType());
                assertTrue(captor.getValue().getDetail().contains("\"action\":\"CALL\""));
                assertTrue(captor.getValue().getDetail().contains("\"status\":\"IN_PROGRESS\""));
                assertTrue(captor.getValue().getDetail().contains("\"callCount\":1"));
        }

        @Test
        void recordsCompletedWithSemanticAction() {
                Instant now = Instant.parse("2026-08-02T02:00:00Z");
                QueueItem item = QueueItem.create(UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                                QueueItemSourceType.WALK_IN, 1, LocalDate.of(2026, 8, 2), UUID.randomUUID(), now);
                item.call(now.plusSeconds(30));
                item.complete(now.plusSeconds(60));
                AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
                CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
                when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());
                when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

                new QueueAuditService(auditLogRepository, currentUserPort).recordCompleted(item);

                ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
                verify(auditLogRepository).save(captor.capture());
                assertEquals(ActionType.UPDATE, captor.getValue().getActionType());
                assertTrue(captor.getValue().getDetail().contains("\"action\":\"COMPLETED\""));
                assertTrue(captor.getValue().getDetail().contains("\"status\":\"COMPLETED\""));
                assertTrue(captor.getValue().getDetail().contains("\"callCount\":1"));
        }

        @Test
        void recordsCancelledWithSemanticActionAndReason() {
                Instant now = Instant.parse("2026-08-02T02:00:00Z");
                QueueItem item = QueueItem.create(UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                                QueueItemSourceType.WALK_IN, 1, LocalDate.of(2026, 8, 2), UUID.randomUUID(), now);
                item.cancel("Patient left clinic", now.plusSeconds(30));
                AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
                CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
                when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());
                when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

                new QueueAuditService(auditLogRepository, currentUserPort).recordCancelled(item, "Patient left clinic");

                ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
                verify(auditLogRepository).save(captor.capture());
                assertEquals(ActionType.CANCEL, captor.getValue().getActionType());
                assertTrue(captor.getValue().getDetail().contains("\"action\":\"CANCELLED\""));
                assertTrue(captor.getValue().getDetail().contains("\"status\":\"CANCELLED\""));
                assertTrue(captor.getValue().getDetail().contains("\"reason\":\"Patient left clinic\""));
                assertTrue(captor.getValue().getDetail().contains("\"callCount\":0"));
        }

        @Test
        void recordsReQueuedWithSemanticAction() {
                Instant now = Instant.parse("2026-08-02T02:00:00Z");
                QueueItem item = QueueItem.create(UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                                QueueItemSourceType.WALK_IN, 1, LocalDate.of(2026, 8, 2), UUID.randomUUID(), now);
                item.call(now.plusSeconds(30));
                item.skip("Absent", now.plusSeconds(60));
                item.reQueue(now.plusSeconds(90));
                AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
                CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
                when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());
                when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

                new QueueAuditService(auditLogRepository, currentUserPort).recordReQueued(item);

                ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
                verify(auditLogRepository).save(captor.capture());
                assertEquals(ActionType.UPDATE, captor.getValue().getActionType());
                assertTrue(captor.getValue().getDetail().contains("\"action\":\"RE_QUEUED\""));
                assertTrue(captor.getValue().getDetail().contains("\"status\":\"WAITING\""));
                assertTrue(captor.getValue().getDetail().contains("\"callCount\":1"));
        }

        @Test
        void recordsPrioritizedQueueItem() {
                Instant now = Instant.parse("2026-08-02T02:00:00Z");
                QueueItem item = QueueItem.create(UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                                QueueItemSourceType.WALK_IN, 1, LocalDate.of(2026, 8, 2), UUID.randomUUID(), now);
                UUID actorId = UUID.randomUUID();
                item.prioritize(com.benhsoan.domain.queue.enums.QueuePriority.EMERGENCY, "Ca cap cuu - Sot cao co giat",
                                actorId, now.plusSeconds(15));

                AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
                CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
                when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
                when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

                new QueueAuditService(auditLogRepository, currentUserPort)
                                .recordPrioritized(item, com.benhsoan.domain.queue.enums.QueuePriority.EMERGENCY,
                                                "Ca cap cuu - Sot cao co giat");

                ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
                verify(auditLogRepository).save(captor.capture());
                assertEquals(ActionType.UPDATE, captor.getValue().getActionType());
                assertTrue(captor.getValue().getDetail().contains("\"action\":\"PRIORITIZED\""));
                assertTrue(captor.getValue().getDetail().contains("\"priority\":\"EMERGENCY\""));
                assertTrue(captor.getValue().getDetail().contains("\"reason\":\"Ca cap cuu - Sot cao co giat\""));
                assertTrue(captor.getValue().getDetail().contains("\"queueItemId\":\"" + item.getId() + "\""));
        }

        @Test
        void recordsAuditWithSpecialCharactersSafely() {
                UUID actorId = UUID.randomUUID();
                QueueItem item = QueueItem.create(UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                                QueueItemSourceType.WALK_IN, 1, LocalDate.of(2026, 8, 2), actorId,
                                Instant.parse("2026-08-02T02:00:00Z"));
                AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
                CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
                when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
                when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

                String specialReason = "Bệnh nhân \"sốt cao co giật\"\nCần cấp cứu ngay!";
                new QueueAuditService(auditLogRepository, currentUserPort)
                                .recordPrioritized(item, com.benhsoan.domain.queue.enums.QueuePriority.EMERGENCY,
                                                specialReason);

                ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
                verify(auditLogRepository).save(captor.capture());
                String detail = captor.getValue().getDetail();

                // Must be parseable valid JSON
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
                        var node = mapper.readTree(detail);
                        assertEquals("PRIORITIZED", node.get("action").asText());
                        assertEquals("EMERGENCY", node.get("priority").asText());
                        assertEquals(specialReason, node.get("reason").asText());
                });
        }

        @Test
        void fallsBackToSafeJsonWhenSerializationFails() throws Exception {
                UUID actorId = UUID.randomUUID();
                QueueItem item = QueueItem.create(UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                                QueueItemSourceType.WALK_IN, 1, LocalDate.of(2026, 8, 2), actorId,
                                Instant.parse("2026-08-02T02:00:00Z"));
                AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
                CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
                when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
                when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

                com.fasterxml.jackson.databind.ObjectMapper faultyMapper = mock(
                                com.fasterxml.jackson.databind.ObjectMapper.class);
                when(faultyMapper.createObjectNode())
                                .thenReturn(new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode());
                when(faultyMapper.writeValueAsString(any()))
                                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("Simulated error") {
                                });

                new QueueAuditService(auditLogRepository, currentUserPort, faultyMapper)
                                .recordPrioritized(item, com.benhsoan.domain.queue.enums.QueuePriority.EMERGENCY,
                                                "Special \"reason\"");

                ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
                verify(auditLogRepository).save(captor.capture());
                String detail = captor.getValue().getDetail();

                // Must still be parseable valid JSON even on fallback
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
                        var node = mapper.readTree(detail);
                        assertEquals("PRIORITIZED", node.get("action").asText());
                        assertEquals("EMERGENCY", node.get("priority").asText());
                });
        }
}
