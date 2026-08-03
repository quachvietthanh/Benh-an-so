package com.benhsoan.persistence.jpaRepository.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.queue.Room;
import com.benhsoan.domain.queue.exception.RoomCodeAlreadyExistsException;
import com.benhsoan.domain.queue.enums.MedicalQueueStatus;
import com.benhsoan.persistence.adapterRepository.queue.RoomRepositoryAdapter;
import com.benhsoan.persistence.entity.queue.MedicalQueueEntity;
import com.benhsoan.persistence.mapper.queue.QueueStructurePersistenceMapper;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class RoomRepositoryJpaIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-02T02:00:00Z");

    @Autowired private JpaRoomRepository jpaRepository;
    @Autowired private JpaMedicalQueueRepository medicalQueueRepository;

    private RoomRepositoryAdapter repository;

    @BeforeEach
    void setUp() {
        repository = new RoomRepositoryAdapter(jpaRepository, new QueueStructurePersistenceMapper());
    }

    @Test
    void searchesByCodeOrNameAndActiveStatus() {
        repository.save(Room.create("P101", "Phong kham Noi", NOW));
        repository.save(Room.create("P102", "Phong kham Nhi", NOW));
        Room inactiveRoom = Room.create("P201", "Phong kham Noi 2", NOW);
        inactiveRoom.deactivate(NOW.plusSeconds(60));
        repository.save(inactiveRoom);

        var activeRooms = repository.search("noi", true, PageRequest.of(0, 20));
        var inactiveRooms = repository.search("P201", false, PageRequest.of(0, 20));

        assertEquals(1, activeRooms.getTotalElements());
        assertEquals("P101", activeRooms.getContent().getFirst().getCode());
        assertEquals(1, inactiveRooms.getTotalElements());
        assertEquals("P201", inactiveRooms.getContent().getFirst().getCode());
    }

    @Test
    void translatesUniqueRoomCodeViolationToConflictException() {
        repository.save(Room.create("P101", "Phong kham 101", NOW));

        assertThrows(RoomCodeAlreadyExistsException.class,
                () -> repository.save(Room.create("p101", "Phong kham trung ma", NOW)));
    }

    @Test
    void deactivatingRoomKeepsExistingMedicalQueueHistory() {
        Room room = repository.save(Room.create("P101", "Phong kham 101", NOW));
        UUID queueId = UUID.randomUUID();
        medicalQueueRepository.saveAndFlush(MedicalQueueEntity.builder()
                .id(queueId)
                .doctorId(UUID.randomUUID())
                .roomId(room.getId())
                .queueDate(LocalDate.of(2026, 8, 2))
                .status(MedicalQueueStatus.OPEN)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build());

        room.deactivate(NOW.plusSeconds(60));
        repository.save(room);

        assertEquals(false, repository.findById(room.getId()).orElseThrow().isActive());
        assertEquals(room.getId(), medicalQueueRepository.findById(queueId).orElseThrow().getRoomId());
    }
}
