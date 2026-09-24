package com.benhsoan.persistence.adapterRepository.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.portal.notification.PatientPortalNotification;
import com.benhsoan.domain.portal.notification.PatientPortalNotificationType;
import com.benhsoan.persistence.jpaRepository.portal.JpaPatientPortalNotificationRepository;
import com.benhsoan.persistence.mapper.portal.PatientPortalNotificationPersistenceMapper;
import com.benhsoan.port.outbound.repository.portal.PatientPortalNotificationRepository;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({PatientPortalNotificationRepositoryAdapter.class, PatientPortalNotificationPersistenceMapper.class})
class PatientPortalNotificationRepositoryAdapterIntegrationTest {

    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID OTHER_PATIENT_ID = UUID.randomUUID();
    private static final UUID APPOINTMENT_ID = UUID.randomUUID();
    private static final UUID RESCHEDULE_LOG_ID = UUID.randomUUID();
    private static final UUID CLINICAL_RESULT_ID = UUID.randomUUID();

    @Autowired
    private PatientPortalNotificationRepository repository;

    @Autowired
    private JpaPatientPortalNotificationRepository jpaRepository;

    @BeforeEach
    void cleanUp() {
        jpaRepository.deleteAll();
    }

    @Test
    void savedNotificationIsReloadedUnchanged() {
        PatientPortalNotification saved = repository.save(PatientPortalNotification.reminder(
                PATIENT_ID, "t", "m", APPOINTMENT_ID, Instant.parse("2026-09-30T01:00:00Z")));

        PatientPortalNotification reloaded = repository.findById(saved.getId()).orElseThrow();

        assertEquals(saved.getId(), reloaded.getId());
        assertEquals(PATIENT_ID, reloaded.getPatientId());
        assertEquals(PatientPortalNotificationType.APPOINTMENT_REMINDER, reloaded.getType());
        assertEquals(APPOINTMENT_ID, reloaded.getAppointmentId());
        assertFalse(reloaded.isRead());
    }

    @Test
    void listsNewestFirstAndAppliesLimit() {
        Instant t1 = Instant.parse("2026-09-28T01:00:00Z");
        Instant t2 = Instant.parse("2026-09-29T01:00:00Z");
        Instant t3 = Instant.parse("2026-09-30T01:00:00Z");
        repository.save(PatientPortalNotification.reminder(PATIENT_ID, "a", "m", UUID.randomUUID(), t1));
        repository.save(PatientPortalNotification.reminder(PATIENT_ID, "b", "m", UUID.randomUUID(), t3));
        repository.save(PatientPortalNotification.reminder(PATIENT_ID, "c", "m", UUID.randomUUID(), t2));

        List<PatientPortalNotification> top2 =
                repository.findByPatientIdOrderByCreatedAtDesc(PATIENT_ID, 2);

        assertEquals(2, top2.size());
        assertEquals(t3, top2.get(0).getCreatedAt());
        assertEquals(t2, top2.get(1).getCreatedAt());
    }

    @Test
    void listIsScopedToPatient() {
        repository.save(PatientPortalNotification.reminder(PATIENT_ID, "a", "m", UUID.randomUUID(),
                Instant.parse("2026-09-30T01:00:00Z")));
        repository.save(PatientPortalNotification.reminder(OTHER_PATIENT_ID, "b", "m", UUID.randomUUID(),
                Instant.parse("2026-09-30T02:00:00Z")));

        List<PatientPortalNotification> mine =
                repository.findByPatientIdOrderByCreatedAtDesc(PATIENT_ID, 10);

        assertEquals(1, mine.size());
        assertEquals(PATIENT_ID, mine.get(0).getPatientId());
    }

    @Test
    void existenceChecksReflectStoredRows() {
        repository.save(PatientPortalNotification.reminder(PATIENT_ID, "t", "m", APPOINTMENT_ID,
                Instant.parse("2026-09-30T01:00:00Z")));
        repository.save(PatientPortalNotification.changed(PATIENT_ID, "t", "m", APPOINTMENT_ID,
                RESCHEDULE_LOG_ID, Instant.parse("2026-09-30T02:00:00Z")));
        repository.save(PatientPortalNotification.labResultAvailable(PATIENT_ID, "t", "m",
                CLINICAL_RESULT_ID, Instant.parse("2026-09-30T03:00:00Z")));

        assertTrue(repository.existsByPatientIdAndTypeAndAppointmentId(
                PATIENT_ID, PatientPortalNotificationType.APPOINTMENT_REMINDER, APPOINTMENT_ID));
        assertTrue(repository.existsByPatientIdAndTypeAndRescheduleLogId(
                PATIENT_ID, PatientPortalNotificationType.APPOINTMENT_CHANGED, RESCHEDULE_LOG_ID));
        assertTrue(repository.existsByPatientIdAndTypeAndClinicalResultId(
                PATIENT_ID, PatientPortalNotificationType.LAB_RESULT_AVAILABLE, CLINICAL_RESULT_ID));

        assertFalse(repository.existsByPatientIdAndTypeAndAppointmentId(
                OTHER_PATIENT_ID, PatientPortalNotificationType.APPOINTMENT_REMINDER, APPOINTMENT_ID));
        assertFalse(repository.existsByPatientIdAndTypeAndClinicalResultId(
                PATIENT_ID, PatientPortalNotificationType.LAB_RESULT_AVAILABLE, UUID.randomUUID()));
    }
}
