package com.benhsoan.persistence.adapterRepository.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.benhsoan.domain.appointment.notification.enums.NotificationChannel;
import com.benhsoan.domain.appointment.notification.enums.NotificationStatus;
import com.benhsoan.domain.appointment.notification.enums.NotificationType;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.patient.enums.AllergySeverity;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.persistence.entity.appointment.AppointmentNotificationLogEntity;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordEntity;
import com.benhsoan.persistence.entity.patient.PatientAllergyChangeLogEntity;
import com.benhsoan.persistence.entity.patient.PatientAllergyEntity;
import com.benhsoan.persistence.entity.visit.VisitEntity;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(PatientMergeDataPersistenceAdapter.class)
@DisplayName("PatientMergeDataPersistenceAdapter - Integration Tests (F-03, F-08, QTN-33)")
class PatientMergeDataPersistenceAdapterTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PatientMergeDataPersistenceAdapter adapter;

    private static final Instant NOW = Instant.parse("2026-09-15T10:00:00Z");

    @Test
    @DisplayName("IT-01 / F-03: Chuyển toàn bộ dữ liệu gồm Visit và AppointmentNotificationLog từ hồ sơ nguồn sang đích")
    void transferAllPatientData_TransfersVisitsAndNotificationLogs() {
        UUID sourcePatientId = UUID.randomUUID();
        UUID targetPatientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();

        // 1. Create a Visit for source patient
        VisitEntity visit = VisitEntity.builder()
                .id(UUID.randomUUID())
                .visitCode("KCB-0001")
                .patientId(sourcePatientId)
                .doctorId(doctorId)
                .visitType(VisitType.WALK_IN)
                .status(VisitStatus.IN_PROGRESS)
                .visitAt(NOW)
                .reason("Khám tổng quát")
                .createdBy(doctorId)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
        entityManager.persist(visit);

        // 2. Create an AppointmentNotificationLog for source patient
        AppointmentNotificationLogEntity notifLog = AppointmentNotificationLogEntity.builder()
                .id(UUID.randomUUID())
                .appointmentId(appointmentId)
                .patientId(sourcePatientId)
                .notificationType(NotificationType.APPOINTMENT_REMINDER)
                .channel(NotificationChannel.MOCK)
                .content("Nhắc lịch hẹn lúc 10h")
                .status(NotificationStatus.SENT)
                .attemptedAt(NOW)
                .sentAt(NOW)
                .createdAt(NOW)
                .build();
        entityManager.persist(notifLog);

        entityManager.flush();
        entityManager.clear();

        // When
        int transferredCount = adapter.transferAllPatientData(sourcePatientId, targetPatientId);

        // Then
        assertEquals(1, transferredCount, "Có 1 lượt khám được chuyển giao");

        VisitEntity updatedVisit = entityManager.find(VisitEntity.class, visit.getId());
        assertEquals(targetPatientId, updatedVisit.getPatientId(), "Visit phải đổi sang targetPatientId");

        AppointmentNotificationLogEntity updatedLog = entityManager.find(AppointmentNotificationLogEntity.class, notifLog.getId());
        assertEquals(targetPatientId, updatedLog.getPatientId(), "NotificationLog phải đổi sang targetPatientId");
    }

    @Test
    @DisplayName("IT-02 / QTN-33: Chuyển dị ứng an toàn, hủy kích hoạt dị ứng trùng tên ở nguồn để tránh xung đột unique")
    void transferAllergiesSafely_DeactivatesDuplicateActiveAllergyInSource() {
        UUID sourcePatientId = UUID.randomUUID();
        UUID targetPatientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        // Target has active Paracetamol allergy
        PatientAllergyEntity targetAllergy = PatientAllergyEntity.builder()
                .id(UUID.randomUUID())
                .patientId(targetPatientId)
                .allergenType("MEDICATION")
                .allergenName("Paracetamol")
                .normalizedAllergenName("paracetamol")
                .severity(AllergySeverity.MILD)
                .reaction("Phát ban nhẹ")
                .active(true)
                .createdBy(doctorId)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
        entityManager.persist(targetAllergy);

        // Source also has active Paracetamol allergy (duplicate)
        PatientAllergyEntity sourceDuplicateAllergy = PatientAllergyEntity.builder()
                .id(UUID.randomUUID())
                .patientId(sourcePatientId)
                .allergenType("MEDICATION")
                .allergenName("Paracetamol 500mg")
                .normalizedAllergenName("paracetamol")
                .severity(AllergySeverity.SEVERE)
                .reaction("Mẩn ngứa toàn thân")
                .active(true)
                .createdBy(doctorId)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
        entityManager.persist(sourceDuplicateAllergy);

        // Source has active Aspirin allergy (non-duplicate)
        PatientAllergyEntity sourceUniqueAllergy = PatientAllergyEntity.builder()
                .id(UUID.randomUUID())
                .patientId(sourcePatientId)
                .allergenType("MEDICATION")
                .allergenName("Aspirin")
                .normalizedAllergenName("aspirin")
                .severity(AllergySeverity.MILD)
                .reaction("Đau dạ dày")
                .active(true)
                .createdBy(doctorId)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
        entityManager.persist(sourceUniqueAllergy);

        entityManager.flush();
        entityManager.clear();

        // When
        adapter.transferAllPatientData(sourcePatientId, targetPatientId);
        entityManager.flush();
        entityManager.clear();

        // Then
        // The duplicate allergy of source should be deactivated (active = false) and transferred to target
        PatientAllergyEntity checkedSourceDuplicate = entityManager.find(PatientAllergyEntity.class, sourceDuplicateAllergy.getId());
        assertEquals(targetPatientId, checkedSourceDuplicate.getPatientId(), "Dị ứng trùng được chuyển sang target");
        assertFalse(checkedSourceDuplicate.isActive(), "Dị ứng trùng bị deactivate để không vi phạm unique constraint");

        // The unique allergy should remain active and transferred to target
        PatientAllergyEntity checkedSourceUnique = entityManager.find(PatientAllergyEntity.class, sourceUniqueAllergy.getId());
        assertEquals(targetPatientId, checkedSourceUnique.getPatientId(), "Dị ứng duy nhất được chuyển sang target");
        assertTrue(checkedSourceUnique.isActive(), "Dị ứng duy nhất vẫn active");

        // Target's original allergy remains active
        PatientAllergyEntity checkedTarget = entityManager.find(PatientAllergyEntity.class, targetAllergy.getId());
        assertTrue(checkedTarget.isActive());
    }

    @Test
    @DisplayName("hasFinalizedMedicalRecords: Trả về true khi có bệnh án SIGNED, LOCKED hoặc ARCHIVED")
    void hasFinalizedMedicalRecords_ReturnsTrueWhenSignedRecordExists() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        VisitEntity visit = VisitEntity.builder()
                .id(UUID.randomUUID())
                .visitCode("KCB-0002")
                .patientId(patientId)
                .doctorId(doctorId)
                .visitType(VisitType.WALK_IN)
                .status(VisitStatus.COMPLETED)
                .visitAt(NOW)
                .reason("Khám mắt")
                .createdBy(doctorId)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
        entityManager.persist(visit);

        MedicalRecordEntity record = MedicalRecordEntity.builder()
                .id(UUID.randomUUID())
                .visitId(visit.getId())
                .status(MedicalRecordStatus.SIGNED)
                .createdBy(doctorId)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
        entityManager.persist(record);

        entityManager.flush();
        entityManager.clear();

        assertTrue(adapter.hasFinalizedMedicalRecords(patientId));
    }
}
