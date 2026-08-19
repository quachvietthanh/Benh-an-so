package com.benhsoan.persistence.adapterRepository.clinic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.persistence.jpaRepository.clinic.JpaClinicConfigurationRepository;
import com.benhsoan.persistence.mapper.clinic.ClinicConfigurationPersistenceMapper;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.datasource.url=jdbc:h2:mem:clinic-configuration-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ClinicConfigurationRepositoryAdapterIntegrationTest {

    @Autowired
    private JpaClinicConfigurationRepository jpaRepository;

    private ClinicConfigurationRepositoryAdapter repository;

    @BeforeEach
    void setUp() {
        repository = new ClinicConfigurationRepositoryAdapter(
                jpaRepository,
                new ClinicConfigurationPersistenceMapper()
        );
    }

    @Test
    void savesAndLoadsTheSingletonConfiguration() {
        ClinicConfiguration configuration = ClinicConfiguration.create(
                "Phong kham Benh So An",
                "Thanh pho Ho Chi Minh",
                "0900000000",
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                Instant.parse("2026-08-01T01:00:00Z")
        );

        repository.save(configuration);

        ClinicConfiguration loaded = repository.find().orElseThrow();
        assertEquals(ClinicConfiguration.SINGLETON_ID, loaded.getId());
        assertEquals("Phong kham Benh So An", loaded.getClinicName());
        assertEquals(LocalTime.of(8, 0), loaded.getOpeningTime());
        assertEquals(LocalTime.of(17, 0), loaded.getClosingTime());
    }
}
