package com.benhsoan.application.ucservice.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.queue.enums.MedicalQueueStatus;
import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.queue.enums.QueuePriority;
import com.benhsoan.persistence.adapterRepository.queue.MedicalQueueRepositoryAdapter;
import com.benhsoan.persistence.adapterRepository.queue.QueueItemRepositoryAdapter;
import com.benhsoan.persistence.entity.queue.MedicalQueueEntity;
import com.benhsoan.persistence.entity.queue.QueueItemEntity;
import com.benhsoan.persistence.jpaRepository.queue.JpaMedicalQueueRepository;
import com.benhsoan.persistence.jpaRepository.queue.JpaQueueItemRepository;
import com.benhsoan.persistence.mapper.queue.MedicalQueuePersistenceMapper;
import com.benhsoan.persistence.mapper.queue.QueueStructurePersistenceMapper;
import com.benhsoan.port.dto.command.queue.PrioritizeQueueItemCommand;
import com.benhsoan.port.outbound.repository.queue.QueueItemQueryRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

/**
 * Verifies transaction atomicity and rollback for PrioritizeQueueItemService (F-06).
 * Ensures that if audit recording fails, the queue item priority change rolls back completely.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({
        PrioritizeQueueItemService.class,
        QueueOperationAuthorization.class,
        QueueItemRepositoryAdapter.class,
        MedicalQueueRepositoryAdapter.class,
        QueueStructurePersistenceMapper.class,
        MedicalQueuePersistenceMapper.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PrioritizeQueueItemTransactionIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-02T02:00:00Z");
    private static final LocalDate QUEUE_DATE = LocalDate.of(2026, 8, 2);

    @Autowired private PrioritizeQueueItemService service;
    @Autowired private JpaQueueItemRepository queueItemRepository;
    @Autowired private JpaMedicalQueueRepository medicalQueueRepository;

    @MockitoBean private QueueAuditService queueAuditService;
    @MockitoBean private QueueItemQueryRepository queueItemQueryRepository;
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private ClockPort clockPort;

    @Test
    void rollsBackQueueItemPriorityWhenAuditRecordingFails() {
        UUID doctorId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID queueId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        medicalQueueRepository.save(MedicalQueueEntity.builder()
                .id(queueId).doctorId(doctorId).roomId(UUID.randomUUID()).queueDate(QUEUE_DATE)
                .status(MedicalQueueStatus.OPEN).createdAt(NOW).updatedAt(NOW).build());

        QueueItemEntity item = new QueueItemEntity();
        item.setId(itemId);
        item.setMedicalQueueId(queueId);
        item.setPatientId(patientId);
        item.setVisitId(visitId);
        item.setSourceType(QueueItemSourceType.WALK_IN);
        item.setStatus(QueueItemStatus.WAITING);
        item.setQueueNumber(1);
        item.setQueueDate(QUEUE_DATE);
        item.setCheckedInAt(NOW);
        item.setPriority(QueuePriority.NORMAL);
        item.setCreatedBy(actorId);
        item.setCreatedAt(NOW);
        item.setUpdatedAt(NOW);
        queueItemRepository.save(item);

        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        when(clockPort.now()).thenReturn(NOW.plusSeconds(30));

        // Simulate audit log write failure inside the transactional boundary
        doThrow(new IllegalStateException("Simulated audit log persistence failure"))
                .when(queueAuditService).recordPrioritized(any(), any(), any());

        assertThrows(IllegalStateException.class,
                () -> service.prioritize(new PrioritizeQueueItemCommand(itemId, QueuePriority.EMERGENCY, "Ca cấp cứu")));

        // Verify that the queue item was rolled back to NORMAL priority
        QueueItemEntity reloaded = queueItemRepository.findById(itemId).orElseThrow();
        assertEquals(QueueItemStatus.WAITING, reloaded.getStatus());
        assertEquals(QueuePriority.NORMAL, reloaded.getPriority());
        assertNull(reloaded.getPriorityReason());
        assertNull(reloaded.getPrioritizedAt());
        assertNull(reloaded.getPrioritizedBy());
    }
}
