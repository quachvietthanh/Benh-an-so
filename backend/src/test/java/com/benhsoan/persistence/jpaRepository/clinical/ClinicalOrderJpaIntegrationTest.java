package com.benhsoan.persistence.jpaRepository.clinical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus;
import com.benhsoan.domain.clinical.enums.ClinicalOrderStatus;
import com.benhsoan.domain.clinical.enums.ClinicalResultDataType;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;
import com.benhsoan.persistence.adapterRepository.clinical.ClinicalOrderItemRepositoryAdapter;
import com.benhsoan.persistence.entity.clinical.ClinicalOrderEntity;
import com.benhsoan.persistence.entity.clinical.ClinicalOrderItemEntity;
import com.benhsoan.persistence.entity.clinical.ClinicalServiceCatalogEntity;
import com.benhsoan.persistence.mapper.clinical.ClinicalOrderItemPersistenceMapper;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ClinicalOrderJpaIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-20T01:00:00Z");

    @Autowired private JpaClinicalOrderRepository clinicalOrderRepository;
    @Autowired private JpaClinicalOrderItemRepository clinicalOrderItemRepository;
    @Autowired private JpaClinicalServiceCatalogRepository clinicalServiceCatalogRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void commitsOrderAndItemsTogetherAndFindsThemByVisit() {
        UUID visitId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        inTransaction(() -> {
            clinicalOrderRepository.save(order(orderId, visitId, "ORD-COMMIT"));
            clinicalOrderItemRepository.save(item(orderId));
        });

        assertEquals(1, clinicalOrderRepository.findByVisitIdOrderByOrderedAtDesc(visitId,
                org.springframework.data.domain.PageRequest.of(0, 20)).getTotalElements());
        assertEquals(1, clinicalOrderItemRepository.findByClinicalOrderIdIn(java.util.List.of(orderId)).size());
    }

    @Test
    void rollsBackOrderAndItemsTogether() {
        UUID visitId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            clinicalOrderRepository.save(order(orderId, visitId, "ORD-ROLLBACK"));
            clinicalOrderItemRepository.save(item(orderId));
            status.setRollbackOnly();
        });

        assertTrue(clinicalOrderRepository.findById(orderId).isEmpty());
        assertTrue(clinicalOrderItemRepository.findByClinicalOrderIdIn(java.util.List.of(orderId)).isEmpty());
    }

    @Test
    void enforcesUniqueOrderCode() {
        inTransaction(() -> clinicalOrderRepository.saveAndFlush(order(UUID.randomUUID(), UUID.randomUUID(), "ORD-UNIQUE")));

        assertThrows(DataIntegrityViolationException.class,
                () -> inTransaction(() -> clinicalOrderRepository.saveAndFlush(
                        order(UUID.randomUUID(), UUID.randomUUID(), "ORD-UNIQUE")
                )));
    }

    @Test
    void findsOnlyCompletedItemsAndMapsThemToServiceMaster() {
        UUID visitId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID completedClinicalServiceId = UUID.randomUUID();
        UUID cancelledClinicalServiceId = UUID.randomUUID();
        UUID completedMasterId = UUID.randomUUID();
        inTransaction(() -> {
            clinicalOrderRepository.save(order(orderId, visitId, "ORD-BILLING"));
            clinicalServiceCatalogRepository.save(clinicalService(
                    completedClinicalServiceId,
                    completedMasterId,
                    "LAB-CBC"
            ));
            clinicalServiceCatalogRepository.save(clinicalService(
                    cancelledClinicalServiceId,
                    UUID.randomUUID(),
                    "LAB-GLU"
            ));
            clinicalOrderItemRepository.save(item(
                    orderId,
                    completedClinicalServiceId,
                    ClinicalOrderItemStatus.COMPLETED,
                    "LAB-CBC"
            ));
            clinicalOrderItemRepository.save(item(
                    orderId,
                    cancelledClinicalServiceId,
                    ClinicalOrderItemStatus.CANCELLED,
                    "LAB-GLU"
            ));
        });
        ClinicalOrderItemRepositoryAdapter adapter = new ClinicalOrderItemRepositoryAdapter(
                clinicalOrderItemRepository,
                new ClinicalOrderItemPersistenceMapper()
        );

        var billableServices = adapter.findBillableByVisitId(visitId);

        assertEquals(1, billableServices.size());
        assertEquals(completedMasterId, billableServices.getFirst().serviceCatalogId());
        assertEquals("Blood test", billableServices.getFirst().serviceName());
    }

    private void inTransaction(Runnable action) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> action.run());
    }

    private ClinicalOrderEntity order(UUID id, UUID visitId, String code) {
        return ClinicalOrderEntity.builder()
                .id(id).orderCode(code).visitId(visitId).medicalRecordId(UUID.randomUUID())
                .patientId(UUID.randomUUID()).orderedBy(UUID.randomUUID()).status(ClinicalOrderStatus.ORDERED)
                .orderedAt(NOW).createdAt(NOW).build();
    }

    private ClinicalOrderItemEntity item(UUID orderId) {
        return item(
                orderId,
                UUID.randomUUID(),
                ClinicalOrderItemStatus.PENDING,
                "LAB-GLU"
        );
    }

    private ClinicalOrderItemEntity item(
            UUID orderId,
            UUID clinicalServiceId,
            ClinicalOrderItemStatus status,
            String serviceCode
    ) {
        return ClinicalOrderItemEntity.builder()
                .id(UUID.randomUUID()).clinicalOrderId(orderId).clinicalServiceId(clinicalServiceId)
                .serviceCode(serviceCode).serviceName("Blood test")
                .status(status).createdAt(NOW).build();
    }

    private ClinicalServiceCatalogEntity clinicalService(
            UUID id,
            UUID serviceCatalogId,
            String serviceCode
    ) {
        return ClinicalServiceCatalogEntity.builder()
                .id(id)
                .serviceCatalogId(serviceCatalogId)
                .serviceCode(serviceCode)
                .serviceName("Blood test")
                .serviceType(ClinicalServiceType.LAB_TEST)
                .resultDataType(ClinicalResultDataType.NUMBER)
                .active(false)
                .createdAt(NOW)
                .build();
    }
}
