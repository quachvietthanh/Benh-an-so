package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.medicalrecord.enums.DiagnosisType;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordRetentionException;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordAccessLogEntity;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordAmendmentEntity;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordDiagnosisEntity;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordEntity;
import com.benhsoan.persistence.entity.visit.VisitEntity;
import com.benhsoan.persistence.jpaRepository.auditlog.JpaAuditLogRepository;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordAccessLogRepository;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordAmendmentRepository;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordDiagnosisRepository;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordRepository;
import com.benhsoan.persistence.jpaRepository.visit.JpaVisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:retention-it;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MedicalRecordRetentionIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-21T00:00:00Z");
    private static final UUID ACTOR = UUID.randomUUID();

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DeleteMedicalRecordService deleteMedicalRecordService;
    @Autowired
    private JpaVisitRepository visits;
    @Autowired
    private JpaMedicalRecordRepository medicalRecords;
    @Autowired
    private JpaMedicalRecordAccessLogRepository accessLogs;
    @Autowired
    private JpaMedicalRecordAmendmentRepository amendments;
    @Autowired
    private JpaMedicalRecordDiagnosisRepository diagnoses;
    @Autowired
    private JpaAuditLogRepository auditLogs;

    @MockitoBean
    private CurrentUserPort currentUserPort;
    @MockitoBean
    private ClockPort clockPort;

    @BeforeAll
    void addForeignKeyConstraints() {
        jdbc.execute("ALTER TABLE medical_record_access_logs "
                + "ADD CONSTRAINT fk_access_record_it FOREIGN KEY (medical_record_id) REFERENCES medical_records(id)");
        jdbc.execute("ALTER TABLE medical_record_amendments "
                + "ADD CONSTRAINT fk_amendments_record_it FOREIGN KEY (medical_record_id) REFERENCES medical_records(id)");
        jdbc.execute("ALTER TABLE medical_record_diagnoses "
                + "ADD CONSTRAINT fk_diagnoses_record_it FOREIGN KEY (medical_record_id) REFERENCES medical_records(id)");
    }

    @BeforeEach
    void stubPorts() {
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR);
    }

    @Test
    void deniesDeletionWithinRetentionAndPersistsDenialAudit() {
        UUID recordId = UUID.randomUUID();
        seedCompletedVisitAndRecord(recordId, NOW.minusSeconds(3600));
        accessLogs.saveAndFlush(accessLog(UUID.randomUUID(), recordId));
        amendments.saveAndFlush(amendment(UUID.randomUUID(), recordId));

        assertThrows(MedicalRecordRetentionException.class,
                () -> deleteMedicalRecordService.delete(recordId));

        assertTrue(medicalRecords.findById(recordId).isPresent());
        assertEquals(1, countAccessLogs(recordId));
        assertEquals(1, amendments.findByMedicalRecordIdOrderByAmendedAtDesc(recordId).size());
        assertEquals(1, countAuditLogs(recordId, ActionType.ACCESS_DENIED));
    }

    @Test
    void deletesExpiredRecordAndCleansUpChildRowsWithoutFkViolation() {
        UUID recordId = UUID.randomUUID();
        seedCompletedVisitAndRecord(recordId, NOW.minusSeconds(11L * 365 * 24 * 3600));
        accessLogs.saveAndFlush(accessLog(UUID.randomUUID(), recordId));
        amendments.saveAndFlush(amendment(UUID.randomUUID(), recordId));
        diagnoses.saveAndFlush(diagnosis(UUID.randomUUID(), recordId));

        deleteMedicalRecordService.delete(recordId);

        assertFalse(medicalRecords.findById(recordId).isPresent());
        assertEquals(0, countAccessLogs(recordId));
        assertEquals(0, amendments.findByMedicalRecordIdOrderByAmendedAtDesc(recordId).size());
        assertEquals(0, diagnoses.findByMedicalRecordId(recordId).size());
        assertEquals(1, countAuditLogs(recordId, ActionType.DELETE));
    }

    @Test
    void uncompletedVisitCannotBeDeleted() {
        UUID recordId = UUID.randomUUID();
        seedUncompletedVisitAndRecord(recordId);

        assertThrows(MedicalRecordRetentionException.class,
                () -> deleteMedicalRecordService.delete(recordId));

        assertTrue(medicalRecords.findById(recordId).isPresent());
    }

    private void seedCompletedVisitAndRecord(UUID recordId, Instant completedAt) {
        UUID visitId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Instant visitAt = completedAt.minusSeconds(3600);
        visits.saveAndFlush(VisitEntity.builder()
                .id(visitId)
                .visitCode("VIS-" + visitId.toString().substring(0, 8))
                .patientId(patientId)
                .doctorId(doctorId)
                .visitType(VisitType.WALK_IN)
                .status(VisitStatus.COMPLETED)
                .visitAt(visitAt)
                .startedAt(visitAt.plusSeconds(60))
                .completedAt(completedAt)
                .reason("Checkup")
                .createdBy(doctorId)
                .createdAt(visitAt)
                .updatedAt(completedAt)
                .build());
        medicalRecords.saveAndFlush(record(recordId, visitId, doctorId, completedAt));
    }

    private void seedUncompletedVisitAndRecord(UUID recordId) {
        UUID visitId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Instant visitAt = NOW.minusSeconds(3600);
        visits.saveAndFlush(VisitEntity.builder()
                .id(visitId)
                .visitCode("VIS-" + visitId.toString().substring(0, 8))
                .patientId(patientId)
                .doctorId(doctorId)
                .visitType(VisitType.WALK_IN)
                .status(VisitStatus.IN_PROGRESS)
                .visitAt(visitAt)
                .startedAt(visitAt.plusSeconds(60))
                .reason("Checkup")
                .createdBy(doctorId)
                .createdAt(visitAt)
                .build());
        medicalRecords.saveAndFlush(record(recordId, visitId, doctorId, NOW));
    }

    private MedicalRecordEntity record(UUID recordId, UUID visitId, UUID doctorId, Instant createdAt) {
        return MedicalRecordEntity.builder()
                .id(recordId)
                .visitId(visitId)
                .chiefComplaint("c")
                .conclusion("co")
                .status(MedicalRecordStatus.LOCKED)
                .createdBy(doctorId)
                .createdAt(createdAt)
                .build();
    }

    private MedicalRecordAccessLogEntity accessLog(UUID id, UUID recordId) {
        return MedicalRecordAccessLogEntity.builder()
                .id(id)
                .patientId(UUID.randomUUID())
                .medicalRecordId(recordId)
                .accessedBy(ACTOR)
                .action(MedicalRecordAccessAction.VIEW)
                .detail("view")
                .accessedAt(NOW.minusSeconds(60))
                .build();
    }

    private MedicalRecordAmendmentEntity amendment(UUID id, UUID recordId) {
        return MedicalRecordAmendmentEntity.builder()
                .id(id)
                .medicalRecordId(recordId)
                .content("content")
                .reason("reason")
                .amendedBy(ACTOR)
                .amendedAt(NOW.minusSeconds(120))
                .build();
    }

    private MedicalRecordDiagnosisEntity diagnosis(UUID id, UUID recordId) {
        return MedicalRecordDiagnosisEntity.builder()
                .id(id)
                .medicalRecordId(recordId)
                .diagnosisName("Diagnosis")
                .diagnosisType(DiagnosisType.PRIMARY)
                .diagnosedBy(ACTOR)
                .diagnosedAt(NOW.minusSeconds(180))
                .createdAt(NOW.minusSeconds(180))
                .build();
    }

    private long countAccessLogs(UUID recordId) {
        return accessLogs.findAll().stream()
                .filter(log -> recordId.equals(log.getMedicalRecordId()))
                .count();
    }

    private long countAuditLogs(UUID recordId, ActionType actionType) {
        return auditLogs.findAll().stream()
                .filter(log -> recordId.equals(log.getResourceId()))
                .filter(log -> actionType == log.getActionType())
                .count();
    }
}
