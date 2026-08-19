package com.benhsoan.application.ucservice.servicecatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.servicecatalog.ServiceCatalog;
import com.benhsoan.domain.servicecatalog.ServicePrice;
import com.benhsoan.port.dto.command.servicecatalog.SearchServiceCatalogQuery;
import com.benhsoan.port.outbound.repository.servicecatalog.ServiceCatalogRepository;
import com.benhsoan.port.outbound.repository.servicecatalog.ServicePriceRepository;

@ExtendWith(MockitoExtension.class)
class ServiceCatalogQueryServicesTest {

    private static final UUID SERVICE_ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private ServiceCatalogRepository serviceCatalogRepository;
    @Mock
    private ServicePriceRepository servicePriceRepository;

    private ServiceCatalog catalog;
    private ServicePrice price;
    private ServiceCatalogResultMapper mapper;

    @BeforeEach
    void setUp() {
        catalog = ServiceCatalog.create(SERVICE_ID, "LAB-CBC", "Công thức máu", CREATED_AT);
        price = ServicePrice.create(
                UUID.randomUUID(),
                SERVICE_ID,
                new BigDecimal("95000.00"),
                LocalDate.of(2026, 1, 1),
                CREATED_AT,
                UUID.randomUUID()
        );
        mapper = new ServiceCatalogResultMapper();
    }

    @Test
    void getReturnsLatestPrice() {
        when(serviceCatalogRepository.findById(SERVICE_ID)).thenReturn(Optional.of(catalog));
        when(servicePriceRepository.findAllByServiceCatalogId(SERVICE_ID)).thenReturn(List.of(price));
        GetServiceCatalogService service = new GetServiceCatalogService(
                serviceCatalogRepository,
                servicePriceRepository,
                mapper
        );

        var result = service.getById(SERVICE_ID);

        assertEquals(0, result.price().compareTo(new BigDecimal("95000.00")));
    }

    @Test
    void searchMapsCatalogAndLatestPrice() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(serviceCatalogRepository.search("cbc", true, pageable))
                .thenReturn(new PageImpl<>(List.of(catalog), pageable, 1));
        when(servicePriceRepository.findAllByServiceCatalogId(SERVICE_ID)).thenReturn(List.of(price));
        SearchServiceCatalogService service = new SearchServiceCatalogService(
                serviceCatalogRepository,
                servicePriceRepository,
                mapper
        );

        var result = service.search(new SearchServiceCatalogQuery("cbc", true, pageable));

        assertEquals(1, result.getTotalElements());
        assertEquals("LAB-CBC", result.getContent().getFirst().serviceCode());
    }

    @Test
    void historyKeepsRepositoryOrder() {
        ServicePrice olderPrice = ServicePrice.create(
                UUID.randomUUID(),
                SERVICE_ID,
                new BigDecimal("80000.00"),
                LocalDate.of(2025, 1, 1),
                CREATED_AT,
                UUID.randomUUID()
        );
        when(serviceCatalogRepository.findById(SERVICE_ID)).thenReturn(Optional.of(catalog));
        when(servicePriceRepository.findAllByServiceCatalogId(SERVICE_ID))
                .thenReturn(List.of(price, olderPrice));
        GetServicePriceHistoryService service = new GetServicePriceHistoryService(
                serviceCatalogRepository,
                servicePriceRepository,
                mapper
        );

        var result = service.getHistory(SERVICE_ID);

        assertEquals(LocalDate.of(2026, 1, 1), result.getFirst().effectiveFrom());
        assertEquals(LocalDate.of(2025, 1, 1), result.getLast().effectiveFrom());
    }
}
