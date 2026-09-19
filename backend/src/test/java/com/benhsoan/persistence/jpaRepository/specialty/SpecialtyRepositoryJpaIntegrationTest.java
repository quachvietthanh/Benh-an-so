package com.benhsoan.persistence.jpaRepository.specialty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.persistence.adapterRepository.specialty.DoctorSpecialtyRepositoryAdapter;
import com.benhsoan.persistence.adapterRepository.specialty.RoomSpecialtyRepositoryAdapter;
import com.benhsoan.persistence.adapterRepository.specialty.SpecialtyRepositoryAdapter;
import com.benhsoan.persistence.mapper.specialty.SpecialtyPersistenceMapper;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class SpecialtyRepositoryJpaIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-18T10:00:00Z");

    @Autowired private JpaSpecialtyRepository jpaSpecialtyRepository;
    @Autowired private JpaDoctorSpecialtyRepository jpaDoctorSpecialtyRepository;
    @Autowired private JpaRoomSpecialtyRepository jpaRoomSpecialtyRepository;

    private SpecialtyRepositoryAdapter specialtyRepository;
    private DoctorSpecialtyRepositoryAdapter doctorSpecialtyRepository;
    private RoomSpecialtyRepositoryAdapter roomSpecialtyRepository;

    @BeforeEach
    void setUp() {
        SpecialtyPersistenceMapper mapper = new SpecialtyPersistenceMapper();
        specialtyRepository = new SpecialtyRepositoryAdapter(jpaSpecialtyRepository, mapper);
        doctorSpecialtyRepository = new DoctorSpecialtyRepositoryAdapter(jpaDoctorSpecialtyRepository);
        roomSpecialtyRepository = new RoomSpecialtyRepositoryAdapter(jpaRoomSpecialtyRepository);
    }

    @Test
    void savesAndRetrievesSpecialtyWithSearch() {
        Specialty s1 = Specialty.create("PEDIATRICS", "Khoa Nhi", "Nhi khoa", NOW);
        Specialty s2 = Specialty.create("SURGERY", "Khoa Ngoại", "Ngoại khoa", NOW);
        specialtyRepository.save(s1);
        specialtyRepository.save(s2);

        assertTrue(specialtyRepository.existsByCode("PEDIATRICS"));
        assertTrue(specialtyRepository.existsByNameKey("khoa nhi"));
        assertFalse(specialtyRepository.existsByNameKey("khoa mắt"));

        List<Specialty> searchResult = specialtyRepository.search("nhi", true);
        assertEquals(1, searchResult.size());
        assertEquals("PEDIATRICS", searchResult.getFirst().getCode());
        assertEquals("Nhi khoa", searchResult.getFirst().getDescription());
    }

    @Test
    void managesDoctorAndRoomAssignments() {
        UUID specialtyId = UUID.randomUUID();
        UUID doctor1 = UUID.randomUUID();
        UUID doctor2 = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID room1 = UUID.randomUUID();

        doctorSpecialtyRepository.assignDoctors(specialtyId, List.of(doctor1, doctor2), adminId, NOW);
        assertEquals(2, doctorSpecialtyRepository.countBySpecialtyId(specialtyId));
        assertEquals(List.of(specialtyId), doctorSpecialtyRepository.findSpecialtyIdsByDoctorId(doctor1));

        roomSpecialtyRepository.assignRooms(specialtyId, List.of(room1), NOW);
        assertEquals(1, roomSpecialtyRepository.countBySpecialtyId(specialtyId));
        assertEquals(List.of(room1), roomSpecialtyRepository.findRoomIdsBySpecialtyId(specialtyId));

        doctorSpecialtyRepository.removeAssignmentsBySpecialtyId(specialtyId);
        assertEquals(0, doctorSpecialtyRepository.countBySpecialtyId(specialtyId));

        roomSpecialtyRepository.removeAssignmentsBySpecialtyId(specialtyId);
        assertEquals(0, roomSpecialtyRepository.countBySpecialtyId(specialtyId));
    }

    @Test
    void savesSpecialtyEntityDirectlyWithoutNameKeyAndAutomaticallyPopulatesIt() {
        UUID specialtyId = UUID.randomUUID();
        com.benhsoan.persistence.entity.specialty.SpecialtyEntity entity = com.benhsoan.persistence.entity.specialty.SpecialtyEntity.builder()
                .id(specialtyId)
                .code("DERMATOLOGY")
                .name("Khoa Da Liễu")
                .active(true)
                .createdAt(NOW)
                .build();

        com.benhsoan.persistence.entity.specialty.SpecialtyEntity saved = jpaSpecialtyRepository.saveAndFlush(entity);

        assertNotNull(saved.getNameKey());
        assertEquals("khoa da liễu", saved.getNameKey());
    }
}
