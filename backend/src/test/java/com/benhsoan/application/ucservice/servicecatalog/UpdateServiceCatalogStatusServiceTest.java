package com.benhsoan.application.ucservice.servicecatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.servicecatalog.ServiceCatalog;
import com.benhsoan.domain.servicecatalog.ServicePrice;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.servicecatalog.ServiceCatalogRepository;
import com.benhsoan.port.outbound.repository.servicecatalog.ServicePriceRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class UpdateServiceCatalogStatusServiceTest {

    private static final UUID SERVICE_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant NOW = Instant.parse("2026-08-18T09:00:00Z");

    @Mock private ServiceCatalogRepository serviceCatalogRepository;
    @Mock private ServicePriceRepository servicePriceRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;

    private UpdateServiceCatalogStatusService service;
    private ServiceCatalog catalog;
    private ServicePrice price;

    @BeforeEach
    void setUp() {
        service = new UpdateServiceCatalogStatusService(
                serviceCatalogRepository,
                servicePriceRepository,
                auditLogRepository,
                currentUserPort,
                clockPort,
                new ServiceCatalogResultMapper()
        );
        catalog = ServiceCatalog.create(SERVICE_ID, "LAB-CBC", "Complete blood count", CREATED_AT);
        price = ServicePrice.create(
                UUID.randomUUID(),
                SERVICE_ID,
                new BigDecimal("95000.00"),
                LocalDate.of(2026, 1, 1),
                CREATED_AT,
                ACTOR_ID
        );
        when(serviceCatalogRepository.findById(SERVICE_ID)).thenReturn(Optional.of(catalog));
        when(servicePriceRepository.findAllByServiceCatalogId(SERVICE_ID)).thenReturn(List.of(price));
    }

    @Test
    void deactivatesCatalogWithoutCreatingPriceAndWritesAudit() {
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
        when(serviceCatalogRepository.save(any(ServiceCatalog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.updateStatus(SERVICE_ID, false);

        assertEquals(false, result.active());
        assertEquals(new BigDecimal("95000.00"), result.price());
        verify(servicePriceRepository, never()).save(any());
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertEquals(ActionType.DEACTIVATE, auditCaptor.getValue().getActionType());
        assertEquals(ResourceType.SERVICE_CATALOG, auditCaptor.getValue().getResourceType());
    }

    @Test
    void unchangedStatusIsNoOp() {
        var result = service.updateStatus(SERVICE_ID, true);

        assertEquals(true, result.active());
        verify(serviceCatalogRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
        verify(clockPort, never()).now();
        verify(currentUserPort, never()).getCurrentUserId();
    }
}
