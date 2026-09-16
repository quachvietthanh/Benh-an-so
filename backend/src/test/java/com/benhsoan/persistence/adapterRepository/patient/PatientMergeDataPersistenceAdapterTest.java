package com.benhsoan.persistence.adapterRepository.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
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
import com.benhsoan.persistence.entity.patient.PatientAllergyEntity;
import com.benhsoan.persistence.entity.patient.PatientChronicDiseaseEntity;
import com.benhsoan.persistence.entity.patient.PatientFamilyHistoryEntity;
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

                AppointmentNotificationLogEntity updatedLog = entityManager.find(AppointmentNotificationLogEntity.class,
                                notifLog.getId());
                assertEquals(targetPatientId, updatedLog.getPatientId(),
                                "NotificationLog phải đổi sang targetPatientId");
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
                // The duplicate allergy of source should be deactivated (active = false) and
                // transferred to target
                PatientAllergyEntity checkedSourceDuplicate = entityManager.find(PatientAllergyEntity.class,
                                sourceDuplicateAllergy.getId());
                assertEquals(targetPatientId, checkedSourceDuplicate.getPatientId(),
                                "Dị ứng trùng được chuyển sang target");
                assertFalse(checkedSourceDuplicate.isActive(),
                                "Dị ứng trùng bị deactivate để không vi phạm unique constraint");

                // The unique allergy should remain active and transferred to target
                PatientAllergyEntity checkedSourceUnique = entityManager.find(PatientAllergyEntity.class,
                                sourceUniqueAllergy.getId());
                assertEquals(targetPatientId, checkedSourceUnique.getPatientId(),
                                "Dị ứng duy nhất được chuyển sang target");
                assertTrue(checkedSourceUnique.isActive(), "Dị ứng duy nhất vẫn active");

                // Target's original allergy remains active
                PatientAllergyEntity checkedTarget = entityManager.find(PatientAllergyEntity.class,
                                targetAllergy.getId());
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

        @Test
        @DisplayName("IT-03 / QTN-33: Chuyển bệnh mạn tính an toàn, hủy kích hoạt bệnh mạn tính trùng lặp ở nguồn")
        void transferChronicDiseasesSafely_TransfersUniqueAndDeactivatesDuplicates() {
                UUID sourcePatientId = UUID.randomUUID();
                UUID targetPatientId = UUID.randomUUID();
                UUID doctorId = UUID.randomUUID();
                UUID duplicateCatalogId = UUID.randomUUID();
                UUID uniqueCatalogId = UUID.randomUUID();

                // Target already has active chronic disease for duplicateCatalogId (e.g.
                // Diabetes)
                PatientChronicDiseaseEntity targetChronic = PatientChronicDiseaseEntity.builder()
                                .id(UUID.randomUUID())
                                .patientId(targetPatientId)
                                .diagnosisCatalogId(duplicateCatalogId)
                                .yearDetected(2020)
                                .notes("Đái tháo đường type 2")
                                .active(true)
                                .createdBy(doctorId)
                                .createdAt(NOW)
                                .updatedAt(NOW)
                                .build();
                entityManager.persist(targetChronic);

                // Source also has active chronic disease for duplicateCatalogId (duplicate)
                PatientChronicDiseaseEntity sourceDuplicate = PatientChronicDiseaseEntity.builder()
                                .id(UUID.randomUUID())
                                .patientId(sourcePatientId)
                                .diagnosisCatalogId(duplicateCatalogId)
                                .yearDetected(2021)
                                .notes("Tiểu đường phát hiện năm ngoái")
                                .active(true)
                                .createdBy(doctorId)
                                .createdAt(NOW)
                                .updatedAt(NOW)
                                .build();
                entityManager.persist(sourceDuplicate);

                // Source has active chronic disease for uniqueCatalogId (unique, e.g.
                // Hypertension)
                PatientChronicDiseaseEntity sourceUnique = PatientChronicDiseaseEntity.builder()
                                .id(UUID.randomUUID())
                                .patientId(sourcePatientId)
                                .diagnosisCatalogId(uniqueCatalogId)
                                .yearDetected(2019)
                                .notes("Tăng huyết áp độ 1")
                                .active(true)
                                .createdBy(doctorId)
                                .createdAt(NOW)
                                .updatedAt(NOW)
                                .build();
                entityManager.persist(sourceUnique);

                entityManager.flush();
                entityManager.clear();

                // When
                adapter.transferAllPatientData(sourcePatientId, targetPatientId);
                entityManager.flush();
                entityManager.clear();

                // Then
                // 1. Source duplicate should be deactivated and reassigned to target
                PatientChronicDiseaseEntity checkedDuplicate = entityManager.find(PatientChronicDiseaseEntity.class,
                                sourceDuplicate.getId());
                assertEquals(targetPatientId, checkedDuplicate.getPatientId(), "Bản ghi trùng chuyển sang target");
                assertFalse(checkedDuplicate.isActive(),
                                "Bản ghi trùng ở source bị deactivate để tránh xung đột unique");

                // 2. Source unique remains active and reassigned to target
                PatientChronicDiseaseEntity checkedUnique = entityManager.find(PatientChronicDiseaseEntity.class,
                                sourceUnique.getId());
                assertEquals(targetPatientId, checkedUnique.getPatientId(), "Bản ghi duy nhất chuyển sang target");
                assertTrue(checkedUnique.isActive(), "Bản ghi duy nhất vẫn active");

                // 3. Target original remains active
                PatientChronicDiseaseEntity checkedTarget = entityManager.find(PatientChronicDiseaseEntity.class,
                                targetChronic.getId());
                assertTrue(checkedTarget.isActive(), "Bản ghi gốc của target vẫn active");
        }

        @Test
        @DisplayName("IT-04 / QTN-33: Chuyển toàn bộ tiền sử bệnh gia đình từ nguồn sang đích")
        void transferFamilyHistories_TransfersAllRecords() {
                UUID sourcePatientId = UUID.randomUUID();
                UUID targetPatientId = UUID.randomUUID();
                UUID doctorId = UUID.randomUUID();
                UUID catalogId1 = UUID.randomUUID();
                UUID catalogId2 = UUID.randomUUID();

                // Source has 2 family history records
                PatientFamilyHistoryEntity motherHistory = PatientFamilyHistoryEntity.builder()
                                .id(UUID.randomUUID())
                                .patientId(sourcePatientId)
                                .relationship("Mẹ")
                                .diagnosisCatalogId(catalogId1)
                                .notes("Mẹ bị hen phế quản")
                                .active(true)
                                .createdBy(doctorId)
                                .createdAt(NOW)
                                .updatedAt(NOW)
                                .build();
                entityManager.persist(motherHistory);

                PatientFamilyHistoryEntity fatherHistory = PatientFamilyHistoryEntity.builder()
                                .id(UUID.randomUUID())
                                .patientId(sourcePatientId)
                                .relationship("Bố")
                                .diagnosisCatalogId(catalogId2)
                                .notes("Bố bị tim mạch")
                                .active(true)
                                .createdBy(doctorId)
                                .createdAt(NOW)
                                .updatedAt(NOW)
                                .build();
                entityManager.persist(fatherHistory);

                entityManager.flush();
                entityManager.clear();

                // When
                adapter.transferAllPatientData(sourcePatientId, targetPatientId);
                entityManager.flush();
                entityManager.clear();

                // Then
                PatientFamilyHistoryEntity checkedMother = entityManager.find(PatientFamilyHistoryEntity.class,
                                motherHistory.getId());
                assertEquals(targetPatientId, checkedMother.getPatientId(), "Tiền sử mẹ chuyển sang target");
                assertTrue(checkedMother.isActive());

                PatientFamilyHistoryEntity checkedFather = entityManager.find(PatientFamilyHistoryEntity.class,
                                fatherHistory.getId());
                assertEquals(targetPatientId, checkedFather.getPatientId(), "Tiền sử bố chuyển sang target");
                assertTrue(checkedFather.isActive());
        }
}
