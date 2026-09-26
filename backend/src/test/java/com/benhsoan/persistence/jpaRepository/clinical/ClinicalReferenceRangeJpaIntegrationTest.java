package com.benhsoan.persistence.jpaRepository.clinical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.persistence.entity.clinical.ClinicalReferenceRangeEntity;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ClinicalReferenceRangeJpaIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-20T01:00:00Z");

    @Autowired
    private JpaClinicalReferenceRangeRepository repository;

    @Test
    void persistsAndReadsReferenceRange() {
        UUID serviceId = UUID.randomUUID();
        ClinicalReferenceRangeEntity saved = repository.save(entity(serviceId, Gender.MALE, 18, 64,
                new BigDecimal("5"), new BigDecimal("10"), true));

        ClinicalReferenceRangeEntity loaded = repository.findById(saved.getId()).orElseThrow();

        assertEquals(serviceId, loaded.getClinicalServiceId());
        assertEquals(Gender.MALE, loaded.getGender());
        assertEquals(18, loaded.getMinAge());
        assertEquals(64, loaded.getMaxAge());
        assertEquals(new BigDecimal("5"), loaded.getLowerBound());
        assertEquals(new BigDecimal("10"), loaded.getUpperBound());
        assertTrue(loaded.isActive());
    }

    @Test
    void activeLookupExcludesInactiveRanges() {
        UUID serviceId = UUID.randomUUID();
        repository.save(entity(serviceId, Gender.MALE, 18, 64, new BigDecimal("5"), new BigDecimal("10"), true));
        repository.save(entity(serviceId, Gender.MALE, 65, 120, new BigDecimal("4"), new BigDecimal("9"), false));

        var active = repository.findByClinicalServiceIdAndActiveTrueOrderByCreatedAtAscIdAsc(serviceId);
        var all = repository.findByClinicalServiceIdOrderByCreatedAtAscIdAsc(serviceId);

        assertEquals(1, active.size());
        assertEquals(2, all.size());
        assertTrue(active.getFirst().isActive());
        assertFalse(all.stream().allMatch(ClinicalReferenceRangeEntity::isActive));
    }

    private ClinicalReferenceRangeEntity entity(UUID serviceId, Gender gender, Integer minAge, Integer maxAge,
            BigDecimal lower, BigDecimal upper, boolean active) {
        return ClinicalReferenceRangeEntity.builder()
                .id(UUID.randomUUID())
                .clinicalServiceId(serviceId)
                .gender(gender)
                .minAge(minAge)
                .maxAge(maxAge)
                .lowerBound(lower)
                .upperBound(upper)
                .active(active)
                .createdAt(NOW)
                .build();
    }
}
