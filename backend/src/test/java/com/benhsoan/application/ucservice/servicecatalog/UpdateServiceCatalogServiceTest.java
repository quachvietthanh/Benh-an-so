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
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.servicecatalog.ServiceCatalog;
import com.benhsoan.domain.servicecatalog.ServicePrice;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.servicecatalog.UpdateServiceCatalogCommand;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.servicecatalog.ServiceCatalogRepository;
import com.benhsoan.port.outbound.repository.servicecatalog.ServicePriceRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class UpdateServiceCatalogServiceTest {

    private static final UUID SERVICE_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant NOW = Instant.parse("2026-08-18T08:00:00Z");
    private static final LocalDate INITIAL_DATE = LocalDate.of(2026, 1, 1);

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

    private UpdateServiceCatalogService service;
    private ServiceCatalog catalog;
    private ServicePrice initialPrice;

    @BeforeEach
    void setUp() {
        service = new UpdateServiceCatalogService(
                serviceCatalogRepository,
                servicePriceRepository,
                auditLogRepository,
                currentUserPort,
                clockPort,
                new ServiceCatalogResultMapper()
        );
        catalog = ServiceCatalog.create(SERVICE_ID, "LAB-CBC", "Công thức máu", CREATED_AT);
        initialPrice = ServicePrice.create(
                UUID.randomUUID(),
                SERVICE_ID,
                new BigDecimal("95000.00"),
                INITIAL_DATE,
                CREATED_AT,
                ACTOR_ID
        );
    }

    @Test
    void unchangedPriceDoesNotCreateAnotherHistoryRow() {
        prepareExistingData();

        var result = service.update(command("Công thức máu", true, "95000.00", INITIAL_DATE));

        assertEquals(INITIAL_DATE, result.effectiveFrom());
        verify(servicePriceRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void newEffectiveDateAppendsPriceAndPreservesExistingPrice() {
        prepareExistingData();
        when(servicePriceRepository.save(any(ServicePrice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        LocalDate newDate = LocalDate.of(2026, 9, 1);

        var result = service.update(command("Công thức máu", true, "120000.00", newDate));

        assertEquals(newDate, result.effectiveFrom());
        assertEquals(0, initialPrice.getPrice().compareTo(new BigDecimal("95000.00")));
        verify(servicePriceRepository).save(any(ServicePrice.class));
        verify(auditLogRepository, org.mockito.Mockito.times(2)).save(any(AuditLog.class));
    }

    @Test
    void sameEffectiveDateWithDifferentPriceIsRejected() {
        prepareExistingData();

        assertThrows(
                ValidationException.class,
                () -> service.update(command("Công thức máu", true, "120000.00", INITIAL_DATE))
        );

        verify(servicePriceRepository, never()).save(any());
    }

    @Test
    void statusChangeCreatesDeactivateAudit() {
        prepareExistingData();
        when(serviceCatalogRepository.save(any(ServiceCatalog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.update(command("Công thức máu", false, "95000.00", INITIAL_DATE));

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertEquals(ActionType.DEACTIVATE, auditCaptor.getValue().getActionType());
    }

    @Test
    void duplicateNormalizedNameIsRejected() {
        prepareExistingData();
        when(serviceCatalogRepository.existsByNormalizedServiceName(
                "tên bị trùng",
                SERVICE_ID
        )).thenReturn(true);

        assertThrows(
                ValidationException.class,
                () -> service.update(command(" Tên  bị trùng ", true, "95000.00", INITIAL_DATE))
        );

        verify(serviceCatalogRepository, never()).save(any());
    }

    private void prepareExistingData() {
        when(serviceCatalogRepository.findById(SERVICE_ID)).thenReturn(java.util.Optional.of(catalog));
        when(servicePriceRepository.findAllByServiceCatalogId(SERVICE_ID)).thenReturn(List.of(initialPrice));
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
    }

    private UpdateServiceCatalogCommand command(
            String name,
            boolean active,
            String price,
            LocalDate effectiveFrom
    ) {
        return new UpdateServiceCatalogCommand(
                SERVICE_ID,
                name,
                active,
                new BigDecimal(price),
                effectiveFrom
        );
    }
}
