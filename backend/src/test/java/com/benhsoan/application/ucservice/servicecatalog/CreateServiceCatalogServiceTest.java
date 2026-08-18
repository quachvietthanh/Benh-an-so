package com.benhsoan.application.ucservice.servicecatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.servicecatalog.ServiceCatalog;
import com.benhsoan.domain.servicecatalog.ServicePrice;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.servicecatalog.CreateServiceCatalogCommand;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.servicecatalog.ServiceCatalogRepository;
import com.benhsoan.port.outbound.repository.servicecatalog.ServicePriceRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class CreateServiceCatalogServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-18T08:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private ServiceCatalogRepository serviceCatalogRepository;
    @Mock
    private ServicePriceRepository servicePriceRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private CurrentUserPort currentUserPort;
    @Mock
    private ClockPort clockPort;

    private CreateServiceCatalogService service;

    @BeforeEach
    void setUp() {
        service = new CreateServiceCatalogService(
                serviceCatalogRepository,
                servicePriceRepository,
                auditLogRepository,
                currentUserPort,
                clockPort,
                new ServiceCatalogResultMapper()
        );
    }

    @Test
    void createsCatalogInitialPriceAndAuditLogs() {
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
        when(serviceCatalogRepository.save(any(ServiceCatalog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(servicePriceRepository.save(any(ServicePrice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(command(new BigDecimal("95000.00")));

        assertEquals("LAB-CBC", result.serviceCode());
        assertEquals(0, result.price().compareTo(new BigDecimal("95000.00")));
        verify(serviceCatalogRepository).save(any(ServiceCatalog.class));
        verify(servicePriceRepository).save(any(ServicePrice.class));
        verify(auditLogRepository, org.mockito.Mockito.times(2)).save(any(AuditLog.class));
    }

    @Test
    void rejectsNormalizedDuplicateName() {
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
        when(serviceCatalogRepository.existsByNormalizedServiceName(
                "công thức máu toàn bộ",
                null
        )).thenReturn(true);

        assertThrows(ValidationException.class, () -> service.create(command(new BigDecimal("95000"))));

        verify(serviceCatalogRepository, never()).save(any());
        verify(servicePriceRepository, never()).save(any());
    }

    @Test
    void rejectsNegativePriceBeforePersistence() {
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);

        assertThrows(ValidationException.class, () -> service.create(command(new BigDecimal("-1"))));

        verify(serviceCatalogRepository, never()).save(any());
    }

    @Test
    void translatesDatabaseCodeConflict() {
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
        when(serviceCatalogRepository.save(any(ServiceCatalog.class))).thenThrow(
                new DataIntegrityViolationException("uk_service_catalog_code")
        );

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.create(command(new BigDecimal("95000")))
        );

        assertEquals("Service code already exists.", exception.getMessage());
    }

    private CreateServiceCatalogCommand command(BigDecimal price) {
        return new CreateServiceCatalogCommand(
                "LAB-CBC",
                "Công thức   máu toàn bộ",
                price,
                LocalDate.of(2026, 8, 18)
        );
    }
}
