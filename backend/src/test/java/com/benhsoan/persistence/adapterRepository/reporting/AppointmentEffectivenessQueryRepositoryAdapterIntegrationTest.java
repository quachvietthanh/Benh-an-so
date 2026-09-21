package com.benhsoan.persistence.adapterRepository.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.persistence.entity.appointment.AppointmentEntity;
import com.benhsoan.persistence.entity.auth.UserEntity;
import com.benhsoan.persistence.entity.patient.PatientEntity;
import com.benhsoan.port.outbound.repository.reporting.AppointmentStatusCountSummary;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.datasource.url=jdbc:h2:mem:appointment-effectiveness-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class AppointmentEffectivenessQueryRepositoryAdapterIntegrationTest {

    private static final Instant FROM = Instant.parse("2026-07-31T17:00:00Z");
    private static final Instant TO = Instant.parse("2026-08-31T17:00:00Z");

    @Autowired
    private EntityManager entityManager;

    @Test
    void countsAppointmentsByStatusWithinPeriod() {
        UUID doctorId = createDoctor();
        UUID patientId = createPatient(doctorId);

        createAppointment(doctorId, patientId, doctorId, AppointmentStatus.COMPLETED, FROM.plusSeconds(60), null);
        createAppointment(doctorId, patientId, doctorId, AppointmentStatus.COMPLETED, FROM.plusSeconds(120), null);
        createAppointment(doctorId, patientId, doctorId, AppointmentStatus.CANCELLED, FROM.plusSeconds(180), null);

        entityManager.flush();
        entityManager.clear();

        List<AppointmentStatusCountSummary> counts = adapter().findStatusCounts(FROM, TO, null, null);

        assertEquals(2, counts.size());
        assertSummary(counts, AppointmentStatus.COMPLETED, 2);
        assertSummary(counts, AppointmentStatus.CANCELLED, 1);
    }

    @Test
    void excludesAppointmentsOutsidePeriod() {
        UUID doctorId = createDoctor();
        UUID patientId = createPatient(doctorId);

        createAppointment(doctorId, patientId, doctorId, AppointmentStatus.COMPLETED, FROM.minusSeconds(60), null);
        createAppointment(doctorId, patientId, doctorId, AppointmentStatus.COMPLETED, TO, null);

        entityManager.flush();
        entityManager.clear();

        assertTrue(adapter().findStatusCounts(FROM, TO, null, null).isEmpty());
    }

    @Test
    void treatsWindowAsHalfOpen() {
        UUID doctorId = createDoctor();
        UUID patientId = createPatient(doctorId);

        createAppointment(doctorId, patientId, doctorId, AppointmentStatus.SCHEDULED, FROM, null);
        createAppointment(doctorId, patientId, doctorId, AppointmentStatus.SCHEDULED, TO.minusSeconds(1), null);

        entityManager.flush();
        entityManager.clear();

        List<AppointmentStatusCountSummary> counts = adapter().findStatusCounts(FROM, TO, null, null);

        assertEquals(1, counts.size());
        assertSummary(counts, AppointmentStatus.SCHEDULED, 2);
    }

    @Test
    void filtersByDoctor() {
        UUID doctorA = createDoctor();
        UUID doctorB = createDoctor();
        UUID patientId = createPatient(doctorA);

        createAppointment(doctorA, patientId, doctorA, AppointmentStatus.COMPLETED, FROM.plusSeconds(60), null);
        createAppointment(doctorB, patientId, doctorA, AppointmentStatus.COMPLETED, FROM.plusSeconds(120), null);

        entityManager.flush();
        entityManager.clear();

        List<AppointmentStatusCountSummary> counts = adapter().findStatusCounts(FROM, TO, doctorA, null);

        assertEquals(1, counts.size());
        assertSummary(counts, AppointmentStatus.COMPLETED, 1);
    }

    @Test
    void filtersByOnlinePortalChannel() {
        UUID doctorId = createDoctor();
        UUID patientId = createPatient(doctorId);

        createAppointment(doctorId, patientId, doctorId, AppointmentStatus.SCHEDULED, FROM.plusSeconds(60), "ONLINE_PORTAL");
        createAppointment(doctorId, patientId, doctorId, AppointmentStatus.SCHEDULED, FROM.plusSeconds(120), null);

        entityManager.flush();
        entityManager.clear();

        List<AppointmentStatusCountSummary> counts = adapter().findStatusCounts(FROM, TO, null, "ONLINE_PORTAL");

        assertEquals(1, counts.size());
        assertSummary(counts, AppointmentStatus.SCHEDULED, 1);
    }

    @Test
    void filtersByReceptionCounterChannel() {
        UUID doctorId = createDoctor();
        UUID patientId = createPatient(doctorId);

        createAppointment(doctorId, patientId, doctorId, AppointmentStatus.SCHEDULED, FROM.plusSeconds(60), null);
        createAppointment(doctorId, patientId, doctorId, AppointmentStatus.SCHEDULED, FROM.plusSeconds(120), "ONLINE_PORTAL");

        entityManager.flush();
        entityManager.clear();

        List<AppointmentStatusCountSummary> counts = adapter().findStatusCounts(FROM, TO, null, "RECEPTION_COUNTER");

        assertEquals(1, counts.size());
        assertSummary(counts, AppointmentStatus.SCHEDULED, 1);
    }

    @Test
    void returnsEmptyWhenNoAppointments() {
        assertTrue(adapter().findStatusCounts(FROM, TO, null, null).isEmpty());
    }

    private void assertSummary(List<AppointmentStatusCountSummary> counts, AppointmentStatus status, long expected) {
        AppointmentStatusCountSummary summary = counts.stream()
                .filter(s -> s.status() == status)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing summary for " + status));
        assertEquals(expected, summary.count());
    }

    private AppointmentEffectivenessQueryRepositoryAdapter adapter() {
        return new AppointmentEffectivenessQueryRepositoryAdapter(entityManager);
    }

    private UUID createDoctor() {
        UUID id = UUID.randomUUID();
        String suffix = id.toString().replace("-", "").substring(0, 8);
        entityManager.persist(UserEntity.builder()
                .id(id)
                .username("doctor." + suffix)
                .passwordHash("hash")
                .fullName("Dr. " + suffix)
                .email("doctor." + suffix + "@example.com")
                .roleId(UUID.randomUUID())
                .active(true)
                .createdAt(Instant.parse("2026-08-01T00:00:00Z"))
                .build());
        return id;
    }

    private UUID createPatient(UUID actorId) {
        UUID id = UUID.randomUUID();
        String suffix = id.toString().replace("-", "").substring(0, 8);
        entityManager.persist(PatientEntity.builder()
                .id(id)
                .patientCode("BN-" + suffix)
                .fullName("Patient " + suffix)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .phone("0900000000")
                .active(true)
                .createdBy(actorId)
                .createdAt(Instant.parse("2026-08-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-08-01T00:00:00Z"))
                .build());
        return id;
    }

    private void createAppointment(
            UUID doctorId,
            UUID patientId,
            UUID createdBy,
            AppointmentStatus status,
            Instant startTime,
            String bookingChannel
    ) {
        entityManager.persist(AppointmentEntity.builder()
                .id(UUID.randomUUID())
                .appointmentCode("AP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .patientId(patientId)
                .doctorId(doctorId)
                .startTime(startTime)
                .endTime(startTime.plusSeconds(1800))
                .status(status)
                .reason("Khám tổng quát")
                .createdBy(createdBy)
                .createdAt(startTime)
                .bookingChannel(bookingChannel)
                .build());
    }
}
