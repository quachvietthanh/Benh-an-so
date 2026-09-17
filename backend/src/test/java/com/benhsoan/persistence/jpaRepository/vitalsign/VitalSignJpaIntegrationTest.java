package com.benhsoan.persistence.jpaRepository.vitalsign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.benhsoan.persistence.entity.vitalsign.VitalSignEntity;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class VitalSignJpaIntegrationTest {

    @Autowired
    private JpaVitalSignRepository repository;

    @Test
    void persistsAndRetrievesVitalSignAndPatientHistoryInOrder() {
        UUID patientId = UUID.randomUUID();
        UUID visit1 = UUID.randomUUID();
        UUID visit2 = UUID.randomUUID();
        Instant time1 = Instant.parse("2026-08-01T10:00:00Z");
        Instant time2 = Instant.parse("2026-08-15T10:00:00Z");

        VitalSignEntity vs1 = VitalSignEntity.builder()
                .id(UUID.randomUUID())
                .visitId(visit1)
                .patientId(patientId)
                .pulse(75)
                .bloodPressureSystolic(120)
                .bloodPressureDiastolic(80)
                .temperature(new BigDecimal("37.0"))
                .weight(new BigDecimal("60.0"))
                .height(new BigDecimal("165.0"))
                .bmi(new BigDecimal("22.0"))
                .abnormal(false)
                .recordedBy(UUID.randomUUID())
                .recordedAt(time1)
                .build();

        VitalSignEntity vs2 = VitalSignEntity.builder()
                .id(UUID.randomUUID())
                .visitId(visit2)
                .patientId(patientId)
                .pulse(110)
                .bloodPressureSystolic(150)
                .bloodPressureDiastolic(95)
                .temperature(new BigDecimal("38.5"))
                .weight(new BigDecimal("60.5"))
                .height(new BigDecimal("165.0"))
                .bmi(new BigDecimal("22.2"))
                .abnormal(true)
                .abnormalFlags("HYPERTENSION,TACHYCARDIA,FEVER")
                .recordedBy(UUID.randomUUID())
                .recordedAt(time2)
                .build();

        repository.save(vs1);
        repository.save(vs2);

        // Find by visit
        var byVisit = repository.findFirstByVisitIdOrderByRecordedAtDesc(visit1);
        assertTrue(byVisit.isPresent());
        assertEquals(75, byVisit.get().getPulse());

        // Find history by patient in chronological order (Ascending)
        List<VitalSignEntity> history = repository.findByPatientIdOrderByRecordedAtAsc(patientId);
        assertEquals(2, history.size());
        assertEquals(visit1, history.get(0).getVisitId());
        assertEquals(visit2, history.get(1).getVisitId());
        assertTrue(history.get(1).isAbnormal());
        assertNotNull(history.get(1).getAbnormalFlags());
    }
}
