package com.benhsoan.persistence.adapterRepository.clinical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus;
import com.benhsoan.domain.clinical.enums.ClinicalOrderStatus;
import com.benhsoan.domain.clinical.enums.ClinicalResultDataType;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;
import com.benhsoan.persistence.entity.clinical.ClinicalOrderEntity;
import com.benhsoan.persistence.entity.clinical.ClinicalOrderItemEntity;
import com.benhsoan.persistence.entity.clinical.ClinicalServiceCatalogEntity;
import com.benhsoan.persistence.jpaRepository.clinical.JpaClinicalOrderItemRepository;
import com.benhsoan.persistence.jpaRepository.clinical.JpaClinicalOrderRepository;
import com.benhsoan.persistence.jpaRepository.clinical.JpaClinicalServiceCatalogRepository;
import com.benhsoan.persistence.mapper.clinical.ClinicalOrderItemPersistenceMapper;
import com.benhsoan.persistence.mapper.clinical.ClinicalOrderPersistenceMapper;
import com.benhsoan.persistence.mapper.clinical.ClinicalServiceCatalogPersistenceMapper;

@ExtendWith(MockitoExtension.class)
class ClinicalPersistenceRepositoryAdapterTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-20T01:00:00Z");

    @Mock
    private JpaClinicalServiceCatalogRepository clinicalServiceCatalogJpaRepository;

    @Mock
    private JpaClinicalOrderRepository clinicalOrderJpaRepository;

    @Mock
    private JpaClinicalOrderItemRepository clinicalOrderItemJpaRepository;

    @Spy
    private ClinicalServiceCatalogPersistenceMapper clinicalServiceCatalogMapper = new ClinicalServiceCatalogPersistenceMapper();

    @Spy
    private ClinicalOrderPersistenceMapper clinicalOrderMapper = new ClinicalOrderPersistenceMapper();

    @Spy
    private ClinicalOrderItemPersistenceMapper clinicalOrderItemMapper = new ClinicalOrderItemPersistenceMapper();

    @InjectMocks
    private ClinicalServiceCatalogRepositoryAdapter clinicalServiceCatalogRepositoryAdapter;

    @InjectMocks
    private ClinicalOrderRepositoryAdapter clinicalOrderRepositoryAdapter;

    @InjectMocks
    private ClinicalOrderItemRepositoryAdapter clinicalOrderItemRepositoryAdapter;

    @Test
    void findsOnlyActiveCatalogServicesUsingNormalizedKeyword() {
        PageRequest pageable = PageRequest.of(0, 20);
        ClinicalServiceCatalogEntity entity = ClinicalServiceCatalogEntity.builder()
                .id(UUID.randomUUID()).serviceCatalogId(UUID.randomUUID())
                .serviceCode("LAB-GLU").serviceName("Blood glucose")
                .serviceType(ClinicalServiceType.LAB_TEST).resultDataType(ClinicalResultDataType.NUMBER)
                .active(true).createdAt(CREATED_AT).build();
        when(clinicalServiceCatalogJpaRepository.findActiveByKeyword("glucose", pageable))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = clinicalServiceCatalogRepositoryAdapter.findActiveByKeyword(" glucose ", pageable);

        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().getFirst().isActive());
        verify(clinicalServiceCatalogJpaRepository).findActiveByKeyword("glucose", pageable);
    }

    @Test
    void findsOrdersByVisitInDescendingOrderPage() {
        UUID visitId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);
        ClinicalOrderEntity entity = ClinicalOrderEntity.builder()
                .id(UUID.randomUUID()).orderCode("ORD-123").visitId(visitId)
                .medicalRecordId(UUID.randomUUID()).patientId(UUID.randomUUID()).orderedBy(UUID.randomUUID())
                .status(ClinicalOrderStatus.ORDERED).orderedAt(CREATED_AT).createdAt(CREATED_AT).build();
        when(clinicalOrderJpaRepository.findByVisitIdOrderByOrderedAtDesc(visitId, pageable))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = clinicalOrderRepositoryAdapter.findByVisitId(visitId, pageable);

        assertEquals("ORD-123", result.getContent().getFirst().getOrderCode());
        verify(clinicalOrderJpaRepository).findByVisitIdOrderByOrderedAtDesc(visitId, pageable);
    }

    @Test
    void bulkLoadsItemsForOrdersWithoutOneQueryPerOrder() {
        UUID firstOrderId = UUID.randomUUID();
        UUID secondOrderId = UUID.randomUUID();
        ClinicalOrderItemEntity entity = ClinicalOrderItemEntity.builder()
                .id(UUID.randomUUID()).clinicalOrderId(firstOrderId).clinicalServiceId(UUID.randomUUID())
                .serviceCode("LAB-GLU").serviceName("Blood glucose")
                .status(ClinicalOrderItemStatus.PENDING).createdAt(CREATED_AT).build();
        when(clinicalOrderItemJpaRepository.findByClinicalOrderIdIn(List.of(firstOrderId, secondOrderId)))
                .thenReturn(List.of(entity));

        var result = clinicalOrderItemRepositoryAdapter.findByClinicalOrderIdIn(List.of(firstOrderId, secondOrderId));

        assertEquals(1, result.size());
        assertEquals(firstOrderId, result.getFirst().getClinicalOrderId());
        verify(clinicalOrderItemJpaRepository).findByClinicalOrderIdIn(List.of(firstOrderId, secondOrderId));
    }
}
