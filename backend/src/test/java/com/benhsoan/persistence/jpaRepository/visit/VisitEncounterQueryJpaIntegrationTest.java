package com.benhsoan.persistence.jpaRepository.visit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.queue.enums.MedicalQueueStatus;
import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.persistence.adapterRepository.visit.VisitEncounterQueryRepositoryAdapter;
import com.benhsoan.persistence.entity.appointment.AppointmentEntity;
import com.benhsoan.persistence.entity.auth.UserEntity;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordEntity;
import com.benhsoan.persistence.entity.patient.PatientEntity;
import com.benhsoan.persistence.entity.queue.MedicalQueueEntity;
import com.benhsoan.persistence.entity.queue.QueueItemEntity;
import com.benhsoan.persistence.entity.queue.RoomEntity;
import com.benhsoan.persistence.entity.visit.VisitEntity;
import com.benhsoan.persistence.jpaRepository.appointment.JpaAppointmentRepository;
import com.benhsoan.persistence.jpaRepository.auth.JpaUserRepository;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordRepository;
import com.benhsoan.persistence.jpaRepository.patient.JpaPatientRepository;
import com.benhsoan.persistence.jpaRepository.queue.JpaMedicalQueueRepository;
import com.benhsoan.persistence.jpaRepository.queue.JpaQueueItemRepository;
import com.benhsoan.persistence.jpaRepository.queue.JpaRoomRepository;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class VisitEncounterQueryJpaIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-03T02:00:00Z");
    private static final LocalDate QUEUE_DATE = LocalDate.of(2026, 8, 3);

    @Autowired private JpaVisitRepository visitRepository;
    @Autowired private JpaPatientRepository patientRepository;
    @Autowired private JpaUserRepository userRepository;
    @Autowired private JpaRoomRepository roomRepository;
    @Autowired private JpaMedicalQueueRepository medicalQueueRepository;
    @Autowired private JpaQueueItemRepository queueItemRepository;
    @Autowired private JpaAppointmentRepository appointmentRepository;
    @Autowired private JpaMedicalRecordRepository medicalRecordRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private VisitEncounterQueryRepositoryAdapter queryRepository;

    @BeforeEach
    void setUp() {
        queryRepository = new VisitEncounterQueryRepositoryAdapter(visitRepository);
    }

    @Test
    void returnsAppointmentEncounterInOneJoinedQuery() {
        EncounterFixture fixture = createFixture(true, true);
        entityManager.flush();
        entityManager.clear();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        var encounter = queryRepository.findByVisitId(fixture.visitId()).orElseThrow();

        assertEquals(1, statistics.getPrepareStatementCount());
        assertEquals("VIS000100", encounter.visit().visitCode());
        assertEquals("BN000100", encounter.patient().patientCode());
        assertEquals("Bac si Nguyen Van B", encounter.doctor().fullName());
        assertEquals("P101", encounter.room().roomNumber());
        assertEquals(3, encounter.queueItem().queueNumber());
        assertEquals("AP000100", encounter.appointment().appointmentCode());
        assertEquals(MedicalRecordStatus.DRAFT, encounter.medicalRecord().status());
    }

    @Test
    void returnsWalkInEncounterWhenOptionalRelationsAreAbsent() {
        EncounterFixture fixture = createFixture(false, false);

        var encounter = queryRepository.findByVisitId(fixture.visitId()).orElseThrow();

        assertEquals(VisitType.WALK_IN, encounter.visit().type());
        assertNull(encounter.queueItem());
        assertNull(encounter.room());
        assertNull(encounter.appointment());
        assertNull(encounter.medicalRecord());
    }

    private EncounterFixture createFixture(boolean withAppointment, boolean withQueueAndRecord) {
        UUID actorId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID appointmentId = withAppointment ? UUID.randomUUID() : null;
        String doctorSuffix = doctorId.toString().substring(0, 8);

        userRepository.save(UserEntity.builder()
                .id(doctorId).username("doctor." + doctorSuffix).passwordHash("hash")
                .fullName("Bac si Nguyen Van B").email("doctor." + doctorSuffix + "@example.com")
                .roleId(UUID.randomUUID()).active(true).createdAt(NOW).build());
        patientRepository.save(PatientEntity.builder()
                .id(patientId).patientCode("BN000100").fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1990, 1, 1)).gender(Gender.MALE).phone("0900000000")
                .active(true).createdBy(actorId).createdAt(NOW).updatedAt(NOW).build());
        visitRepository.save(VisitEntity.builder()
                .id(visitId).visitCode("VIS000100").patientId(patientId).doctorId(doctorId)
                .appointmentId(appointmentId).visitType(withAppointment ? VisitType.APPOINTMENT : VisitType.WALK_IN)
                .status(VisitStatus.IN_PROGRESS).visitAt(NOW).startedAt(NOW.plusSeconds(60))
                .reason("Kham tong quat").note("Theo doi").createdBy(actorId).createdAt(NOW).build());

        if (withAppointment) {
            appointmentRepository.save(AppointmentEntity.builder()
                    .id(appointmentId).appointmentCode("AP000100").patientId(patientId).doctorId(doctorId)
                    .startTime(NOW).endTime(NOW.plusSeconds(1800)).status(AppointmentStatus.IN_PROGRESS)
                    .reason("Kham tong quat").createdBy(actorId).createdAt(NOW).build());
        }
        if (withQueueAndRecord) {
            UUID roomId = UUID.randomUUID();
            UUID queueId = UUID.randomUUID();
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
            QueueItemEntity queueItem = new QueueItemEntity();
            queueItem.setId(UUID.randomUUID());
            queueItem.setMedicalQueueId(queueId);
            queueItem.setPatientId(patientId);
            queueItem.setAppointmentId(appointmentId);
            queueItem.setVisitId(visitId);
            queueItem.setSourceType(withAppointment ? QueueItemSourceType.APPOINTMENT : QueueItemSourceType.WALK_IN);
            queueItem.setStatus(QueueItemStatus.IN_PROGRESS);
            queueItem.setQueueNumber(3);
            queueItem.setQueueDate(QUEUE_DATE);
            queueItem.setCheckedInAt(NOW);
            queueItem.setCalledAt(NOW.plusSeconds(60));
            queueItem.setCreatedBy(actorId);
            queueItem.setCreatedAt(NOW);
            queueItem.setUpdatedAt(NOW);
            queueItemRepository.save(queueItem);
            medicalRecordRepository.save(MedicalRecordEntity.builder()
                    .id(UUID.randomUUID()).visitId(visitId).status(MedicalRecordStatus.DRAFT)
                    .createdBy(doctorId).createdAt(NOW).build());
        }
        return new EncounterFixture(visitId);
    }

    private record EncounterFixture(UUID visitId) {
    }
}
