package com.benhsoan.persistence.jpaRepository.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.queue.enums.MedicalQueueStatus;
import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.persistence.entity.auth.UserEntity;
import com.benhsoan.persistence.entity.patient.PatientEntity;
import com.benhsoan.persistence.entity.queue.MedicalQueueEntity;
import com.benhsoan.persistence.entity.queue.QueueItemEntity;
import com.benhsoan.persistence.entity.queue.RoomEntity;
import com.benhsoan.persistence.entity.visit.VisitEntity;
import com.benhsoan.persistence.jpaRepository.auth.JpaUserRepository;
import com.benhsoan.persistence.jpaRepository.patient.JpaPatientRepository;
import com.benhsoan.persistence.jpaRepository.visit.JpaVisitRepository;

import jakarta.persistence.EntityManagerFactory;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class QueueItemReadModelJpaIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-02T02:00:00Z");
    private static final LocalDate QUEUE_DATE = LocalDate.of(2026, 8, 2);

    @Autowired private JpaQueueItemRepository queueItemRepository;
    @Autowired private JpaMedicalQueueRepository medicalQueueRepository;
    @Autowired private JpaRoomRepository roomRepository;
    @Autowired private JpaPatientRepository patientRepository;
    @Autowired private JpaUserRepository userRepository;
    @Autowired private JpaVisitRepository visitRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @Test
    void returnsDenormalizedQueueBoardAndDetailInJoinedProjection() {
        UUID doctorId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID queueId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID queueItemId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        userRepository.save(UserEntity.builder()
                .id(doctorId).username("doctor.queue").passwordHash("hash").fullName("Bac si Nguyen Van B")
                .email("doctor.queue@example.com").roleId(UUID.randomUUID()).active(true).createdAt(NOW).build());
        patientRepository.save(PatientEntity.builder()
                .id(patientId).patientCode("BN900001").fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1990, 1, 1)).gender(Gender.MALE).active(true)
                .createdBy(actorId).createdAt(NOW).updatedAt(NOW).build());

        RoomEntity room = new RoomEntity();
        room.setId(roomId);
        room.setCode("P101");
        room.setName("Phong kham 101");
        room.setActive(true);
        room.setCreatedAt(NOW);
        roomRepository.save(room);

        medicalQueueRepository.save(MedicalQueueEntity.builder()
                .id(queueId).doctorId(doctorId).roomId(roomId).queueDate(QUEUE_DATE)
                .status(MedicalQueueStatus.OPEN).createdAt(NOW).updatedAt(NOW).build());
        visitRepository.save(VisitEntity.builder()
                .id(visitId).visitCode("VIS900001").patientId(patientId).doctorId(doctorId)
                .visitType(VisitType.WALK_IN).status(VisitStatus.WAITING).visitAt(NOW)
                .reason("Kham tong quat").createdBy(actorId).createdAt(NOW).build());

        QueueItemEntity item = new QueueItemEntity();
        item.setId(queueItemId);
        item.setMedicalQueueId(queueId);
        item.setPatientId(patientId);
        item.setVisitId(visitId);
        item.setSourceType(QueueItemSourceType.WALK_IN);
        item.setStatus(QueueItemStatus.WAITING);
        item.setQueueNumber(1);
        item.setQueueDate(QUEUE_DATE);
        item.setCheckedInAt(NOW);
        item.setCreatedBy(actorId);
        item.setCreatedAt(NOW);
        item.setUpdatedAt(NOW);
        queueItemRepository.saveAndFlush(item);

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var board = queueItemRepository.findQueueBoardDetails(QUEUE_DATE, doctorId, roomId);
        assertEquals(1, statistics.getPrepareStatementCount());
        var detail = queueItemRepository.findQueueItemDetailsById(queueItemId);

        assertEquals(1, board.size());
        assertEquals("Nguyen Van A", board.getFirst().patientName());
        assertEquals("Bac si Nguyen Van B", board.getFirst().doctorName());
        assertEquals("P101", board.getFirst().roomNumber());
        assertEquals("VIS900001", board.getFirst().visitCode());
        assertTrue(detail.isPresent());
        assertEquals(doctorId, detail.orElseThrow().doctorId());

        item.setStatus(QueueItemStatus.SKIPPED);
        item.setSkippedAt(NOW.plusSeconds(60));
        item.setSkipReason("Patient absent when called");
        queueItemRepository.saveAndFlush(item);
        assertTrue(queueItemRepository.findWaitingForUpdate(queueId).isEmpty());
        var skippedDetail = queueItemRepository.findQueueItemDetailsById(queueItemId).orElseThrow();
        assertEquals(NOW.plusSeconds(60), skippedDetail.skippedAt());
        assertEquals("Patient absent when called", skippedDetail.skipReason());
    }

    @Test
    void ordersQueueBoardAndWaitingItemsByPriorityThenPrioritizedAtThenQueueNumber() {
        UUID doctorId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID queueId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        userRepository.save(UserEntity.builder()
                .id(doctorId).username("doctor.order").passwordHash("hash").fullName("Bac si C")
                .email("doctor.order@example.com").roleId(UUID.randomUUID()).active(true).createdAt(NOW).build());

        RoomEntity room = new RoomEntity();
        room.setId(roomId);
        room.setCode("P102");
        room.setName("Phong kham 102");
        room.setActive(true);
        room.setCreatedAt(NOW);
        roomRepository.save(room);

        medicalQueueRepository.save(MedicalQueueEntity.builder()
                .id(queueId).doctorId(doctorId).roomId(roomId).queueDate(QUEUE_DATE)
                .status(MedicalQueueStatus.OPEN).createdAt(NOW).updatedAt(NOW).build());

        // Create 4 patients with visits
        // P1: NORMAL, queue 1
        // P2: NORMAL, queue 2
        // P3: EMERGENCY, queue 3, prioritized at NOW + 20s
        // P4: EMERGENCY, queue 4, prioritized at NOW + 10s
        createQueueItemEntity(queueId, doctorId, actorId, "BN01", "Patient 1", 1, com.benhsoan.domain.queue.enums.QueuePriority.NORMAL, null);
        createQueueItemEntity(queueId, doctorId, actorId, "BN02", "Patient 2", 2, com.benhsoan.domain.queue.enums.QueuePriority.NORMAL, null);
        createQueueItemEntity(queueId, doctorId, actorId, "BN03", "Patient 3", 3, com.benhsoan.domain.queue.enums.QueuePriority.EMERGENCY, NOW.plusSeconds(20));
        createQueueItemEntity(queueId, doctorId, actorId, "BN04", "Patient 4", 4, com.benhsoan.domain.queue.enums.QueuePriority.EMERGENCY, NOW.plusSeconds(10));

        var board = queueItemRepository.findQueueBoardDetails(QUEUE_DATE, doctorId, roomId);
        assertEquals(4, board.size());
        assertEquals("Patient 4", board.get(0).patientName(), "Patient 4 (EMERGENCY earlier) must be first (AC-01, AC-03)");
        assertEquals("Patient 3", board.get(1).patientName(), "Patient 3 (EMERGENCY later) must be second (AC-03)");
        assertEquals("Patient 1", board.get(2).patientName(), "Patient 1 (NORMAL queue 1) must be third");
        assertEquals("Patient 2", board.get(3).patientName(), "Patient 2 (NORMAL queue 2) must be fourth");

        var waitingForUpdate = queueItemRepository.findWaitingForUpdate(queueId);
        assertEquals(4, waitingForUpdate.size());
        assertEquals(4, waitingForUpdate.get(0).getQueueNumber());
        assertEquals(3, waitingForUpdate.get(1).getQueueNumber());
        assertEquals(1, waitingForUpdate.get(2).getQueueNumber());
        assertEquals(2, waitingForUpdate.get(3).getQueueNumber());
    }

    private void createQueueItemEntity(UUID queueId, UUID doctorId, UUID actorId, String pCode, String pName, int queueNum,
                                       com.benhsoan.domain.queue.enums.QueuePriority priority, Instant prioritizedAt) {
        UUID pId = UUID.randomUUID();
        UUID vId = UUID.randomUUID();
        patientRepository.save(PatientEntity.builder()
                .id(pId).patientCode(pCode).fullName(pName)
                .dateOfBirth(LocalDate.of(1990, 1, 1)).gender(Gender.MALE).active(true)
                .createdBy(actorId).createdAt(NOW).updatedAt(NOW).build());
        visitRepository.save(VisitEntity.builder()
                .id(vId).visitCode("VIS" + pCode).patientId(pId).doctorId(doctorId)
                .visitType(VisitType.WALK_IN).status(VisitStatus.WAITING).visitAt(NOW)
                .reason("Kham").createdBy(actorId).createdAt(NOW).build());
        QueueItemEntity item = new QueueItemEntity();
        item.setId(UUID.randomUUID());
        item.setMedicalQueueId(queueId);
        item.setPatientId(pId);
        item.setVisitId(vId);
        item.setSourceType(QueueItemSourceType.WALK_IN);
        item.setStatus(QueueItemStatus.WAITING);
        item.setQueueNumber(queueNum);
        item.setQueueDate(QUEUE_DATE);
        item.setCheckedInAt(NOW);
        item.setPriority(priority);
        item.setPrioritizedAt(prioritizedAt);
        if (priority != com.benhsoan.domain.queue.enums.QueuePriority.NORMAL) {
            item.setPriorityReason("Ly do uu tien");
            item.setPrioritizedBy(actorId);
        }
        item.setCreatedBy(actorId);
        item.setCreatedAt(NOW);
        item.setUpdatedAt(NOW);
        queueItemRepository.saveAndFlush(item);
    }
}
