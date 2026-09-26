package com.benhsoan.persistence.adapterRepository.followup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.followup.FollowUpReminder;
import com.benhsoan.domain.followup.enums.ReminderStatus;
import com.benhsoan.domain.followup.enums.ReminderType;
import com.benhsoan.persistence.jpaRepository.followup.JpaFollowUpReminderRepository;
import com.benhsoan.persistence.mapper.followup.FollowUpReminderPersistenceMapper;
import com.benhsoan.port.dto.command.followup.SearchFollowUpRemindersQuery;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.datasource.url=jdbc:h2:mem:followup-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class FollowUpReminderRepositoryAdapterIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-15T08:00:00Z");
    private static final UUID PATIENT_A = UUID.randomUUID();
    private static final UUID PATIENT_B = UUID.randomUUID();

    @Autowired
    private JpaFollowUpReminderRepository jpaRepository;

    private FollowUpReminderRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FollowUpReminderRepositoryAdapter(jpaRepository, new FollowUpReminderPersistenceMapper());
    }

    @Test
    void roundTripsReminder() {
        FollowUpReminder reminder = FollowUpReminder.create(
                PATIENT_A, null, null, LocalDate.of(2026, 8, 30), NOW,
                ReminderType.REVISIT, "Recheck", UUID.randomUUID(), NOW);
        adapter.save(reminder);

        Optional<FollowUpReminder> found = adapter.findById(reminder.getId());

        assertTrue(found.isPresent());
        assertEquals(ReminderStatus.PENDING, found.get().getStatus());
        assertEquals(PATIENT_A, found.get().getPatientId());
    }

    @Test
    void findsDueRemindersOnlyPendingAndRemindAtNotAfterNow() {
        adapter.save(FollowUpReminder.create(
                PATIENT_A, null, null, LocalDate.of(2026, 8, 14), NOW.minusSeconds(60),
                ReminderType.GENERAL, null, UUID.randomUUID(), NOW.minusSeconds(60)));
        adapter.save(FollowUpReminder.create(
                PATIENT_B, null, null, LocalDate.of(2026, 8, 20), NOW.plusSeconds(3600),
                ReminderType.GENERAL, null, UUID.randomUUID(), NOW));
        FollowUpReminder sent = FollowUpReminder.create(
                PATIENT_A, null, null, LocalDate.of(2026, 8, 14), NOW.minusSeconds(60),
                ReminderType.GENERAL, null, UUID.randomUUID(), NOW.minusSeconds(60));
        sent.updateStatus(ReminderStatus.SENT);
        adapter.save(sent);

        Page<FollowUpReminder> due = adapter.findDue(NOW, null, null, PageRequest.of(0, 20));

        assertEquals(1, due.getTotalElements());
        assertEquals(PATIENT_A, due.getContent().get(0).getPatientId());
    }

    @Test
    void searchesByPatientAndStatus() {
        adapter.save(FollowUpReminder.create(
                PATIENT_A, null, null, LocalDate.of(2026, 8, 30), NOW,
                ReminderType.GENERAL, null, UUID.randomUUID(), NOW));
        adapter.save(FollowUpReminder.create(
                PATIENT_B, null, null, LocalDate.of(2026, 8, 30), NOW,
                ReminderType.GENERAL, null, UUID.randomUUID(), NOW));

        Page<FollowUpReminder> byPatient = adapter.search(
                new SearchFollowUpRemindersQuery(PATIENT_B, null, null, null, PageRequest.of(0, 20)));
        assertEquals(1, byPatient.getTotalElements());
        assertEquals(PATIENT_B, byPatient.getContent().get(0).getPatientId());

        Page<FollowUpReminder> byStatus = adapter.search(
                new SearchFollowUpRemindersQuery(null, ReminderStatus.PENDING, null, null, PageRequest.of(0, 20)));
        assertEquals(2, byStatus.getTotalElements());
    }
}
