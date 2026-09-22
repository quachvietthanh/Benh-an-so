package com.benhsoan.application.ucservice.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.servicecatalog.ServicePrice;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.outbound.repository.clinical.BillableClinicalService;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderItemRepository;
import com.benhsoan.port.outbound.repository.servicecatalog.ServicePriceRepository;

class ClinicalServiceFeeCalculatorTest {

    @Test
    void usesHoChiMinhInvoiceDateToResolveEffectivePrice() {
        ClinicalOrderItemRepository itemRepository = mock(ClinicalOrderItemRepository.class);
        ServicePriceRepository priceRepository = mock(ServicePriceRepository.class);
        ClinicalServiceFeeCalculator calculator = new ClinicalServiceFeeCalculator(
                itemRepository,
                priceRepository
        );
        UUID visitId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID serviceCatalogId = UUID.randomUUID();
        Instant invoiceAt = Instant.parse("2026-08-18T17:30:00Z");
        LocalDate localInvoiceDate = LocalDate.of(2026, 8, 19);
        when(itemRepository.findBillableByVisitId(visitId)).thenReturn(List.of(
                new BillableClinicalService(itemId, serviceCatalogId, "Blood test")
        ));
        when(priceRepository.findEffectivePrice(serviceCatalogId, localInvoiceDate))
                .thenReturn(Optional.of(price(serviceCatalogId, "125000", localInvoiceDate)));

        List<ClinicalServiceCharge> charges = calculator.calculate(visitId, invoiceAt);

        assertEquals(1, charges.size());
        assertEquals(new BigDecimal("125000"), calculator.total(charges));
        verify(priceRepository).findEffectivePrice(serviceCatalogId, localInvoiceDate);
    }

    @Test
    void rejectsBillableItemWithoutAnEffectivePrice() {
        ClinicalOrderItemRepository itemRepository = mock(ClinicalOrderItemRepository.class);
        ServicePriceRepository priceRepository = mock(ServicePriceRepository.class);
        ClinicalServiceFeeCalculator calculator = new ClinicalServiceFeeCalculator(
                itemRepository,
                priceRepository
        );
        UUID visitId = UUID.randomUUID();
        UUID serviceCatalogId = UUID.randomUUID();
        when(itemRepository.findBillableByVisitId(visitId)).thenReturn(List.of(
                new BillableClinicalService(UUID.randomUUID(), serviceCatalogId, "Blood test")
        ));

        assertThrows(
                ValidationException.class,
                () -> calculator.calculate(visitId, Instant.parse("2026-08-18T10:00:00Z"))
        );
    }

    @Test
    void calculateBatchUsesBulkEffectivePricesAndAggregatesPerVisit() {
        ClinicalOrderItemRepository itemRepository = mock(ClinicalOrderItemRepository.class);
        ServicePriceRepository priceRepository = mock(ServicePriceRepository.class);
        ClinicalServiceFeeCalculator calculator = new ClinicalServiceFeeCalculator(
                itemRepository,
                priceRepository
        );

        UUID visitId1 = UUID.randomUUID();
        UUID visitId2 = UUID.randomUUID();
        UUID visitId3 = UUID.randomUUID();
        UUID catalogId1 = UUID.randomUUID();
        UUID catalogId2 = UUID.randomUUID();

        Instant billingAt = Instant.parse("2026-08-18T17:30:00Z");
        LocalDate billingDate = LocalDate.of(2026, 8, 19);

        when(itemRepository.findBillableByVisitIdIn(List.of(visitId1, visitId2, visitId3)))
                .thenReturn(List.of(
                        new BillableClinicalService(visitId1, UUID.randomUUID(), catalogId1, "Blood test"),
                        new BillableClinicalService(visitId1, UUID.randomUUID(), catalogId2, "X-Ray"),
                        new BillableClinicalService(visitId2, UUID.randomUUID(), catalogId1, "Blood test")
                ));

        when(priceRepository.findEffectivePrices(java.util.Set.of(catalogId1, catalogId2), billingDate))
                .thenReturn(java.util.Map.of(
                        catalogId1, price(catalogId1, "50000", billingDate),
                        catalogId2, price(catalogId2, "120000", billingDate)
                ));

        java.util.Map<UUID, BigDecimal> totals = calculator.calculateBatch(
                List.of(visitId1, visitId2, visitId3),
                billingAt
        );

        assertEquals(new BigDecimal("170000"), totals.get(visitId1));
        assertEquals(new BigDecimal("50000"), totals.get(visitId2));
        assertEquals(BigDecimal.ZERO, totals.get(visitId3));
        verify(priceRepository).findEffectivePrices(java.util.Set.of(catalogId1, catalogId2), billingDate);
    }

    @Test
    void calculateBatchHandlesMissingEffectivePricesGracefully() {
        ClinicalOrderItemRepository itemRepository = mock(ClinicalOrderItemRepository.class);
        ServicePriceRepository priceRepository = mock(ServicePriceRepository.class);
        ClinicalServiceFeeCalculator calculator = new ClinicalServiceFeeCalculator(
                itemRepository,
                priceRepository
        );

        UUID visitId = UUID.randomUUID();
        UUID catalogIdWithPrice = UUID.randomUUID();
        UUID catalogIdWithoutPrice = UUID.randomUUID();

        Instant billingAt = Instant.parse("2026-08-18T10:00:00Z");
        LocalDate billingDate = LocalDate.of(2026, 8, 18);

        when(itemRepository.findBillableByVisitIdIn(List.of(visitId)))
                .thenReturn(List.of(
                        new BillableClinicalService(visitId, UUID.randomUUID(), catalogIdWithPrice, "Blood test"),
                        new BillableClinicalService(visitId, UUID.randomUUID(), catalogIdWithoutPrice, "Rare test")
                ));

        when(priceRepository.findEffectivePrices(java.util.Set.of(catalogIdWithPrice, catalogIdWithoutPrice), billingDate))
                .thenReturn(java.util.Map.of(
                        catalogIdWithPrice, price(catalogIdWithPrice, "80000", billingDate)
                ));

        java.util.Map<UUID, BigDecimal> totals = calculator.calculateBatch(List.of(visitId), billingAt);

        assertEquals(new BigDecimal("80000"), totals.get(visitId));
    }

    private ServicePrice price(UUID serviceCatalogId, String amount, LocalDate effectiveFrom) {
        return ServicePrice.create(
                UUID.randomUUID(),
                serviceCatalogId,
                new BigDecimal(amount),
                effectiveFrom,
                Instant.parse("2026-08-01T00:00:00Z"),
                UUID.randomUUID()
        );
    }
}
