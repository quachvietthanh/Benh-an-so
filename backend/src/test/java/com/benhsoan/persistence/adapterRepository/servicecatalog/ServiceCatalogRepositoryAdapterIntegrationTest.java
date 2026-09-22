package com.benhsoan.persistence.adapterRepository.servicecatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.benhsoan.domain.servicecatalog.ServiceCatalog;
import com.benhsoan.domain.servicecatalog.ServicePrice;
import com.benhsoan.persistence.jpaRepository.servicecatalog.JpaServiceCatalogRepository;
import com.benhsoan.persistence.jpaRepository.servicecatalog.JpaServicePriceRepository;
import com.benhsoan.persistence.mapper.servicecatalog.ServiceCatalogPersistenceMapper;
import com.benhsoan.persistence.mapper.servicecatalog.ServicePricePersistenceMapper;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.datasource.url=jdbc:h2:mem:service-catalog-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ServiceCatalogRepositoryAdapterIntegrationTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private JpaServiceCatalogRepository catalogJpaRepository;

    @Autowired
    private JpaServicePriceRepository priceJpaRepository;

    private ServiceCatalogRepositoryAdapter catalogRepository;
    private ServicePriceRepositoryAdapter priceRepository;

    @BeforeEach
    void setUp() {
        catalogRepository = new ServiceCatalogRepositoryAdapter(
                catalogJpaRepository,
                new ServiceCatalogPersistenceMapper()
        );
        priceRepository = new ServicePriceRepositoryAdapter(
                priceJpaRepository,
                new ServicePricePersistenceMapper()
        );
    }

    @Test
    void roundTripsServiceAndChecksNormalizedName() {
        UUID serviceId = UUID.randomUUID();
        ServiceCatalog service = ServiceCatalog.create(
                serviceId,
                "LAB-CBC",
                "Công thức máu toàn bộ",
                CREATED_AT
        );

        catalogRepository.save(service);

        assertEquals(serviceId, catalogRepository.findByServiceCode("lab-cbc").orElseThrow().getId());
        assertTrue(catalogRepository.existsByServiceCode("lab-cbc"));
        assertTrue(catalogRepository.existsByNormalizedServiceName("  CÔNG THỨC MÁU TOÀN BỘ ", null));
        assertFalse(catalogRepository.existsByNormalizedServiceName("Công thức máu toàn bộ", serviceId));
    }

    @Test
    void findsEffectivePriceAndKeepsHistoryInDescendingOrder() {
        UUID serviceId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        catalogRepository.save(ServiceCatalog.create(serviceId, "IMG-CXR", "X-quang ngực", CREATED_AT));

        priceRepository.save(price(serviceId, actorId, "120000.00", LocalDate.of(2026, 1, 1)));
        priceRepository.save(price(serviceId, actorId, "150000.00", LocalDate.of(2026, 3, 1)));

        ServicePrice februaryPrice = priceRepository
                .findEffectivePrice(serviceId, LocalDate.of(2026, 2, 15))
                .orElseThrow();
        ServicePrice marchPrice = priceRepository
                .findEffectivePrice(serviceId, LocalDate.of(2026, 3, 1))
                .orElseThrow();

        assertEquals(0, februaryPrice.getPrice().compareTo(new BigDecimal("120000.00")));
        assertEquals(0, marchPrice.getPrice().compareTo(new BigDecimal("150000.00")));
        assertTrue(priceRepository.findEffectivePrice(serviceId, LocalDate.of(2025, 12, 31)).isEmpty());
        assertEquals(LocalDate.of(2026, 3, 1),
                priceRepository.findAllByServiceCatalogId(serviceId).getFirst().getEffectiveFrom());
        assertTrue(priceRepository.existsByServiceCatalogIdAndEffectiveFrom(
                serviceId,
                LocalDate.of(2026, 1, 1)
        ));
    }

    @Test
    void findsEffectivePricesInBulkForMultipleServices() {
        UUID serviceId1 = UUID.randomUUID();
        UUID serviceId2 = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        catalogRepository.save(ServiceCatalog.create(serviceId1, "SRV-001", "Dich vu 1", CREATED_AT));
        catalogRepository.save(ServiceCatalog.create(serviceId2, "SRV-002", "Dich vu 2", CREATED_AT));

        priceRepository.save(price(serviceId1, actorId, "100000.00", LocalDate.of(2026, 1, 1)));
        priceRepository.save(price(serviceId1, actorId, "120000.00", LocalDate.of(2026, 2, 1)));

        priceRepository.save(price(serviceId2, actorId, "200000.00", LocalDate.of(2026, 1, 15)));
        priceRepository.save(price(serviceId2, actorId, "250000.00", LocalDate.of(2026, 3, 1)));

        var prices = priceRepository.findEffectivePrices(
                List.of(serviceId1, serviceId2),
                LocalDate.of(2026, 2, 15)
        );

        assertEquals(2, prices.size());
        assertEquals(0, prices.get(serviceId1).getPrice().compareTo(new BigDecimal("120000.00")));
        assertEquals(0, prices.get(serviceId2).getPrice().compareTo(new BigDecimal("200000.00")));
    }


    private ServicePrice price(UUID serviceId, UUID actorId, String amount, LocalDate effectiveFrom) {
        return ServicePrice.create(
                UUID.randomUUID(),
                serviceId,
                new BigDecimal(amount),
                effectiveFrom,
                CREATED_AT,
                actorId
        );
    }
}
