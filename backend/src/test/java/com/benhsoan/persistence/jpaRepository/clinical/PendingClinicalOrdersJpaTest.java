package com.benhsoan.persistence.jpaRepository.clinical;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus;
import com.benhsoan.domain.clinical.enums.ClinicalOrderStatus;
import com.benhsoan.domain.clinical.enums.ClinicalResultDataType;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.persistence.entity.auth.UserEntity;
import com.benhsoan.persistence.entity.clinical.ClinicalOrderEntity;
import com.benhsoan.persistence.entity.clinical.ClinicalOrderItemEntity;
import com.benhsoan.persistence.entity.clinical.ClinicalServiceCatalogEntity;
import com.benhsoan.persistence.entity.patient.PatientEntity;
import com.benhsoan.persistence.entity.visit.VisitEntity;
import com.benhsoan.persistence.jpaRepository.auth.JpaUserRepository;
import com.benhsoan.persistence.jpaRepository.patient.JpaPatientRepository;
import com.benhsoan.persistence.jpaRepository.visit.JpaVisitRepository;

@DataJpaTest(properties = {
                "spring.flyway.enabled=false",
                "spring.sql.init.mode=never",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@DisplayName("PendingClinicalOrdersJpaTest - Kiểm thử JPA queries findPendingItems và findPendingServiceNamesByVisitId")
class PendingClinicalOrdersJpaTest {

        private static final Instant NOW = Instant.parse("2026-08-20T10:00:00Z");

        @Autowired
        private JpaClinicalOrderItemRepository clinicalOrderItemRepository;
        @Autowired
        private JpaClinicalOrderRepository clinicalOrderRepository;
        @Autowired
        private JpaClinicalServiceCatalogRepository serviceCatalogRepository;
        @Autowired
        private JpaVisitRepository visitRepository;
        @Autowired
        private JpaPatientRepository patientRepository;
        @Autowired
        private JpaUserRepository userRepository;

        private UUID doctorId;
        private UUID patientId;
        private UUID activeVisitId;
        private UUID completedVisitId;
        private UUID serviceId;

        @BeforeEach
        void setUp() {
                doctorId = UUID.randomUUID();
                userRepository.save(UserEntity.builder()
                                .id(doctorId)
                                .username("dr_test_" + doctorId)
                                .passwordHash("hashed")
                                .fullName("Bác Sĩ Test")
                                .email("dr_" + doctorId + "@clinic.com")
                                .roleId(UUID.randomUUID())
                                .active(true)
                                .createdAt(NOW)
                                .build());

                patientId = UUID.randomUUID();
                patientRepository.save(PatientEntity.builder()
                                .id(patientId)
                                .patientCode("BN-TEST-001")
                                .fullName("Bệnh Nhân A")
                                .dateOfBirth(LocalDate.of(1990, 1, 1))
                                .gender(Gender.MALE)
                                .active(true)
                                .consentAgreed(true)
                                .consentWithdrawn(false)
                                .nonMedicalUseRestricted(false)
                                .createdBy(doctorId)
                                .createdAt(NOW)
                                .updatedAt(NOW)
                                .build());

                serviceId = UUID.randomUUID();
                serviceCatalogRepository.save(ClinicalServiceCatalogEntity.builder()
                                .id(serviceId)
                                .serviceCatalogId(UUID.randomUUID())
                                .serviceCode("XRAY-01")
                                .serviceName("X-Quang tim phổi")
                                .serviceType(ClinicalServiceType.IMAGING)
                                .resultDataType(ClinicalResultDataType.TEXT)
                                .active(true)
                                .createdAt(NOW)
                                .build());

                activeVisitId = UUID.randomUUID();
                visitRepository.save(VisitEntity.builder()
                                .id(activeVisitId)
                                .visitCode("VIS-ACTIVE")
                                .patientId(patientId)
                                .doctorId(doctorId)
                                .createdBy(doctorId)
                                .reason("Khám tim phổi định kỳ")
                                .visitType(VisitType.WALK_IN)
                                .status(VisitStatus.IN_PROGRESS)
                                .visitAt(NOW)
                                .startedAt(NOW)
                                .createdAt(NOW)
                                .build());

                completedVisitId = UUID.randomUUID();
                visitRepository.save(VisitEntity.builder()
                                .id(completedVisitId)
                                .visitCode("VIS-COMPLETED")
                                .patientId(patientId)
                                .doctorId(doctorId)
                                .createdBy(doctorId)
                                .reason("Khám tai mũi họng cũ")
                                .visitType(VisitType.WALK_IN)
                                .status(VisitStatus.COMPLETED)
                                .visitAt(NOW.minusSeconds(86400))
                                .startedAt(NOW.minusSeconds(86400))
                                .completedAt(NOW.minusSeconds(80000))
                                .createdAt(NOW.minusSeconds(86400))
                                .build());
        }

        @Test
        @DisplayName("findPendingItems chỉ trả về items của lượt khám đang ACTIVE, loại bỏ lượt khám đã đóng (P2-01)")
        void findPendingItemsExcludesFinishedVisits() {
                // Active order & item
                UUID activeOrderId = UUID.randomUUID();
                clinicalOrderRepository.save(ClinicalOrderEntity.builder()
                                .id(activeOrderId)
                                .orderCode("ORD-ACTIVE")
                                .visitId(activeVisitId)
                                .medicalRecordId(UUID.randomUUID())
                                .patientId(patientId)
                                .orderedBy(doctorId)
                                .status(ClinicalOrderStatus.ORDERED)
                                .orderedAt(NOW)
                                .createdAt(NOW)
                                .build());

                clinicalOrderItemRepository.save(ClinicalOrderItemEntity.builder()
                                .id(UUID.randomUUID())
                                .clinicalOrderId(activeOrderId)
                                .clinicalServiceId(serviceId)
                                .serviceCode("XRAY-01")
                                .serviceName("X-Quang tim phổi")
                                .status(ClinicalOrderItemStatus.PENDING)
                                .createdAt(NOW)
                                .build());

                // Finished visit with forgotten pending item
                UUID oldOrderId = UUID.randomUUID();
                clinicalOrderRepository.save(ClinicalOrderEntity.builder()
                                .id(oldOrderId)
                                .orderCode("ORD-OLD")
                                .visitId(completedVisitId)
                                .medicalRecordId(UUID.randomUUID())
                                .patientId(patientId)
                                .orderedBy(doctorId)
                                .status(ClinicalOrderStatus.ORDERED)
                                .orderedAt(NOW.minusSeconds(86400))
                                .createdAt(NOW.minusSeconds(86400))
                                .build());

                clinicalOrderItemRepository.save(ClinicalOrderItemEntity.builder()
                                .id(UUID.randomUUID())
                                .clinicalOrderId(oldOrderId)
                                .clinicalServiceId(serviceId)
                                .serviceCode("XRAY-01")
                                .serviceName("X-Quang tim phổi")
                                .status(ClinicalOrderItemStatus.PENDING)
                                .createdAt(NOW.minusSeconds(86400))
                                .build());

                // When: Query pending items
                Page<PendingClinicalOrderItemView> page = clinicalOrderItemRepository.findPendingItems(
                                ClinicalOrderItemStatus.PENDING,
                                null,
                                null,
                                null,
                                null,
                                PageRequest.of(0, 20));

                // Then: Only active visit item is returned!
                assertEquals(1, page.getTotalElements());
                assertEquals("ORD-ACTIVE", page.getContent().getFirst().getOrderCode());
        }

        @Test
        @DisplayName("findPendingServiceNamesByVisitId trả về danh sách distinct không trùng lặp (P3-02)")
        void findPendingServiceNamesByVisitIdReturnsDistinctList() {
                UUID orderId = UUID.randomUUID();
                clinicalOrderRepository.save(ClinicalOrderEntity.builder()
                                .id(orderId)
                                .orderCode("ORD-MULTI")
                                .visitId(activeVisitId)
                                .medicalRecordId(UUID.randomUUID())
                                .patientId(patientId)
                                .orderedBy(doctorId)
                                .status(ClinicalOrderStatus.ORDERED)
                                .orderedAt(NOW)
                                .createdAt(NOW)
                                .build());

                // 2 items with the EXACT same service name
                clinicalOrderItemRepository.save(ClinicalOrderItemEntity.builder()
                                .id(UUID.randomUUID())
                                .clinicalOrderId(orderId)
                                .clinicalServiceId(serviceId)
                                .serviceCode("XRAY-01")
                                .serviceName("X-Quang tim phổi")
                                .status(ClinicalOrderItemStatus.PENDING)
                                .createdAt(NOW)
                                .build());

                clinicalOrderItemRepository.save(ClinicalOrderItemEntity.builder()
                                .id(UUID.randomUUID())
                                .clinicalOrderId(orderId)
                                .clinicalServiceId(serviceId)
                                .serviceCode("XRAY-01")
                                .serviceName("X-Quang tim phổi")
                                .status(ClinicalOrderItemStatus.PENDING)
                                .createdAt(NOW.plusSeconds(10))
                                .build());

                List<String> names = clinicalOrderItemRepository.findPendingServiceNamesByVisitId(activeVisitId,
                                ClinicalOrderItemStatus.PENDING);

                assertEquals(1, names.size(), "Dịch vụ trùng lặp phải được deduplicate bằng distinct");
                assertEquals("X-Quang tim phổi", names.getFirst());
        }
}
