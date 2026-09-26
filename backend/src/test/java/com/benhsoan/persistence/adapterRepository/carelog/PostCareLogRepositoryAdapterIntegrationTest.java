package com.benhsoan.persistence.adapterRepository.carelog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.carelog.PostCareLog;
import com.benhsoan.domain.carelog.enums.ContactChannel;
import com.benhsoan.domain.carelog.enums.ContactOutcome;
import com.benhsoan.domain.carelog.enums.PatientCondition;
import com.benhsoan.persistence.jpaRepository.carelog.JpaPostCareLogRepository;
import com.benhsoan.persistence.mapper.carelog.PostCareLogPersistenceMapper;
import com.benhsoan.port.dto.command.carelog.SearchPostCareLogsQuery;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.datasource.url=jdbc:h2:mem:carelog-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class PostCareLogRepositoryAdapterIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-15T08:00:00Z");
    private static final UUID PATIENT_A = UUID.randomUUID();
    private static final UUID PATIENT_B = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();

    @Autowired
    private JpaPostCareLogRepository jpaRepository;

    private PostCareLogRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PostCareLogRepositoryAdapter(jpaRepository, new PostCareLogPersistenceMapper());
    }

    @Test
    void roundTripsAndOrdersByContactedAtDesc() {
        PostCareLog older = careLog(PATIENT_A, ContactChannel.PHONE, NOW.minusSeconds(60));
        PostCareLog newer = careLog(PATIENT_A, ContactChannel.ZALO, NOW);
        adapter.save(older);
        adapter.save(newer);

        List<PostCareLog> logs = adapter.findByPatientIdOrderByContactedAtDesc(PATIENT_A);

        assertEquals(2, logs.size());
        assertEquals(newer.getId(), logs.get(0).getId());
        assertEquals(older.getId(), logs.get(1).getId());
        assertEquals(ContactChannel.ZALO, logs.get(0).getContactChannel());
    }

    @Test
    void searchesByChannelAndDateRange() {
        adapter.save(careLog(PATIENT_A, ContactChannel.PHONE, NOW.minusSeconds(60)));
        adapter.save(careLog(PATIENT_A, ContactChannel.ZALO, NOW));
        adapter.save(careLog(PATIENT_B, ContactChannel.PHONE, NOW));

        Page<PostCareLog> byChannel = adapter.search(
                new SearchPostCareLogsQuery(ContactChannel.PHONE, null, null, PageRequest.of(0, 20)));
        assertEquals(2, byChannel.getTotalElements());

        Page<PostCareLog> byRange = adapter.search(
                new SearchPostCareLogsQuery(null, NOW.minusSeconds(30), NOW.plusSeconds(1), PageRequest.of(0, 20)));
        assertEquals(2, byRange.getTotalElements());
    }

    private PostCareLog careLog(UUID patientId, ContactChannel channel, Instant contactedAt) {
        return PostCareLog.create(
                patientId, null, null, channel, contactedAt,
                PatientCondition.STABLE, "Benh nhan on dinh",
                ContactOutcome.REACHED, ACTOR, contactedAt);
    }
}
