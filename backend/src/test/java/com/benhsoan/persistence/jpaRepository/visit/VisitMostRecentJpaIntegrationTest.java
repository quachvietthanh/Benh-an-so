package com.benhsoan.persistence.jpaRepository.visit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.persistence.entity.visit.VisitEntity;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class VisitMostRecentJpaIntegrationTest {

    private static final UUID DOCTOR = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
    private static final Instant BASE = Instant.parse("2026-08-25T00:00:00Z");

    @Autowired
    private JpaVisitRepository repository;

    @Test
    void returnsEmptyForNoEligibleVisits() {
        assertTrue(repository.findFirstByDoctorIdAndStatusNotOrderByVisitAtDescIdDesc(
                DOCTOR, VisitStatus.CANCELLED).isEmpty());
    }

    @Test
    void returnsTheSingleEligibleVisit() {
        VisitEntity visit = saveVisit("VIS-1", VisitStatus.COMPLETED, BASE);

        var result = repository.findFirstByDoctorIdAndStatusNotOrderByVisitAtDescIdDesc(
                DOCTOR, VisitStatus.CANCELLED);

        assertEquals(visit.getId(), result.orElseThrow().getId());
    }

    @Test
    void returnsMostRecentAmongMultipleVisits() {
        saveVisit("VIS-OLD", VisitStatus.COMPLETED, BASE.minusSeconds(60));
        VisitEntity newest = saveVisit("VIS-NEW", VisitStatus.COMPLETED, BASE);

        var result = repository.findFirstByDoctorIdAndStatusNotOrderByVisitAtDescIdDesc(
                DOCTOR, VisitStatus.CANCELLED);

        assertEquals(newest.getId(), result.orElseThrow().getId());
    }

    @Test
    void skipsLatestCancelledVisitAndReturnsPreviousEligible() {
        saveVisit("VIS-CANCELLED", VisitStatus.CANCELLED, BASE);
        VisitEntity previous = saveVisit("VIS-PREVIOUS", VisitStatus.COMPLETED, BASE.minusSeconds(60));

        var result = repository.findFirstByDoctorIdAndStatusNotOrderByVisitAtDescIdDesc(
                DOCTOR, VisitStatus.CANCELLED);

        assertEquals(previous.getId(), result.orElseThrow().getId());
    }

    @Test
    void equalVisitAtIsDeterministicAcrossCalls() {
        VisitEntity first = saveVisit("VIS-A", VisitStatus.COMPLETED, BASE);
        VisitEntity second = saveVisit("VIS-B", VisitStatus.COMPLETED, BASE);

        var result1 = repository.findFirstByDoctorIdAndStatusNotOrderByVisitAtDescIdDesc(
                DOCTOR, VisitStatus.CANCELLED).orElseThrow();
        var result2 = repository.findFirstByDoctorIdAndStatusNotOrderByVisitAtDescIdDesc(
                DOCTOR, VisitStatus.CANCELLED).orElseThrow();

        assertEquals(result1.getId(), result2.getId());
        assertTrue(result1.getId().equals(first.getId()) || result1.getId().equals(second.getId()));
    }

    private VisitEntity saveVisit(String visitCode, VisitStatus status, Instant visitAt) {
        return repository.save(VisitEntity.builder()
                .id(UUID.randomUUID())
                .visitCode(visitCode)
                .patientId(UUID.randomUUID())
                .doctorId(DOCTOR)
                .visitType(VisitType.WALK_IN)
                .status(status)
                .visitAt(visitAt)
                .reason("Exam")
                .createdBy(DOCTOR)
                .createdAt(visitAt)
                .build());
    }
}
