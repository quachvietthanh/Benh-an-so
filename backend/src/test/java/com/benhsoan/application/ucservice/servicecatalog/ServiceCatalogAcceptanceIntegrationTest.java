package com.benhsoan.application.ucservice.servicecatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.persistence.adapterRepository.auditlog.AuditLogRepositoryAdapter;
import com.benhsoan.persistence.adapterRepository.servicecatalog.ServiceCatalogRepositoryAdapter;
import com.benhsoan.persistence.adapterRepository.servicecatalog.ServicePriceRepositoryAdapter;
import com.benhsoan.persistence.jpaRepository.auditlog.JpaAuditLogRepository;
import com.benhsoan.persistence.jpaRepository.servicecatalog.JpaServiceCatalogRepository;
import com.benhsoan.persistence.jpaRepository.servicecatalog.JpaServicePriceRepository;
import com.benhsoan.persistence.mapper.auditlog.AuditLogPersistenceMapper;
import com.benhsoan.persistence.mapper.servicecatalog.ServiceCatalogPersistenceMapper;
import com.benhsoan.persistence.mapper.servicecatalog.ServicePricePersistenceMapper;
import com.benhsoan.port.dto.command.servicecatalog.CreateServiceCatalogCommand;
import com.benhsoan.port.dto.command.servicecatalog.UpdateServiceCatalogCommand;
import com.benhsoan.port.dto.result.servicecatalog.ServiceCatalogResult;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({
        CreateServiceCatalogService.class,
        UpdateServiceCatalogService.class,
        ServiceCatalogResultMapper.class,
        ServiceCatalogRepositoryAdapter.class,
        ServicePriceRepositoryAdapter.class,
        AuditLogRepositoryAdapter.class,
        ServiceCatalogPersistenceMapper.class,
        ServicePricePersistenceMapper.class,
        AuditLogPersistenceMapper.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ServiceCatalogAcceptanceIntegrationTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa9");
    private static final Instant NOW = Instant.parse("2026-08-18T08:00:00Z");
    private static final LocalDate INITIAL_DATE = LocalDate.of(2026, 8, 1);
    private static final LocalDate CHANGED_DATE = LocalDate.of(2026, 9, 1);

    @Autowired private CreateServiceCatalogService createService;
    @Autowired private UpdateServiceCatalogService updateService;
    @Autowired private ServicePriceRepositoryAdapter priceRepository;
    @Autowired private JpaServiceCatalogRepository catalogJpaRepository;
    @Autowired private JpaServicePriceRepository priceJpaRepository;
    @Autowired private JpaAuditLogRepository auditLogJpaRepository;

    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private ClockPort clockPort;

    @BeforeEach
    void setUp() {
        auditLogJpaRepository.deleteAll();
        priceJpaRepository.deleteAll();
        catalogJpaRepository.deleteAll();
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
        when(clockPort.now()).thenReturn(NOW);
    }

    @Test
    void tc01CreatesServiceWithInitialPriceAndAuditLogs() {
        ServiceCatalogResult result = createService.create(createCommand(
                "LAB-CBC",
                "Complete blood count",
                "95000.00"
        ));

        assertEquals("LAB-CBC", result.serviceCode());
        assertMoney("95000.00", result.price());
        assertEquals(INITIAL_DATE, result.effectiveFrom());
        assertEquals(1, catalogJpaRepository.count());
        assertEquals(1, priceJpaRepository.count());

        var audits = auditLogJpaRepository.findAll();
        assertEquals(2, audits.size());
        assertTrue(audits.stream().anyMatch(audit ->
                audit.getActionType() == ActionType.CREATE
                        && audit.getResourceType() == ResourceType.SERVICE_CATALOG));
        assertTrue(audits.stream().anyMatch(audit ->
                audit.getActionType() == ActionType.CREATE
                        && audit.getResourceType() == ResourceType.SERVICE_PRICE));
    }

    @Test
    void tc02RejectsNameDuplicatedAfterNormalization() {
        createService.create(createCommand("LAB-CBC", "Complete blood count", "95000.00"));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> createService.create(createCommand(
                        "LAB-CBC-2",
                        "  COMPLETE   BLOOD   COUNT  ",
                        "100000.00"
                ))
        );

        assertEquals("Service name already exists.", exception.getMessage());
        assertEquals(1, catalogJpaRepository.count());
        assertEquals(1, priceJpaRepository.count());
    }

    @Test
    void tc03RejectsNegativePriceWithoutPersistingAnything() {
        assertThrows(
                ValidationException.class,
                () -> createService.create(createCommand("LAB-CBC", "Complete blood count", "-1.00"))
        );

        assertEquals(0, catalogJpaRepository.count());
        assertEquals(0, priceJpaRepository.count());
        assertEquals(0, auditLogJpaRepository.count());
    }

    @Test
    void tc04PriceChangeAppendsHistoryAndSelectsPriceByEffectiveDate() {
        ServiceCatalogResult created = createService.create(createCommand(
                "LAB-CBC",
                "Complete blood count",
                "95000.00"
        ));

        updateService.update(new UpdateServiceCatalogCommand(
                created.id(),
                created.serviceName(),
                true,
                new BigDecimal("120000.00"),
                CHANGED_DATE
        ));

        var history = priceRepository.findAllByServiceCatalogId(created.id());
        assertEquals(2, history.size());
        assertEquals(CHANGED_DATE, history.get(0).getEffectiveFrom());
        assertMoney("120000.00", history.get(0).getPrice());
        assertEquals(INITIAL_DATE, history.get(1).getEffectiveFrom());
        assertMoney("95000.00", history.get(1).getPrice());

        assertMoney("95000.00", priceRepository
                .findEffectivePrice(created.id(), CHANGED_DATE.minusDays(1))
                .orElseThrow().getPrice());
        assertMoney("120000.00", priceRepository
                .findEffectivePrice(created.id(), CHANGED_DATE)
                .orElseThrow().getPrice());
        assertMoney("120000.00", priceRepository
                .findEffectivePrice(created.id(), CHANGED_DATE.plusDays(30))
                .orElseThrow().getPrice());

        var audits = auditLogJpaRepository.findAll();
        assertEquals(4, audits.size());
        assertTrue(audits.stream().anyMatch(audit ->
                audit.getActionType() == ActionType.UPDATE
                        && audit.getResourceType() == ResourceType.SERVICE_CATALOG));
        assertEquals(2, audits.stream().filter(audit ->
                audit.getActionType() == ActionType.CREATE
                        && audit.getResourceType() == ResourceType.SERVICE_PRICE).count());
    }

    @Test
    void concurrentDuplicateCreateAllowsExactlyOneRequest() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Object> first = executor.submit(() -> attemptCreate(
                    createCommand("LAB-RACE", "Concurrent service A", "10000.00"),
                    ready,
                    start
            ));
            Future<Object> second = executor.submit(() -> attemptCreate(
                    createCommand("LAB-RACE", "Concurrent service B", "20000.00"),
                    ready,
                    start
            ));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            List<Object> outcomes = List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS)
            );
            assertEquals(1, outcomes.stream().filter(ServiceCatalogResult.class::isInstance).count());
            assertEquals(1, outcomes.stream().filter(ValidationException.class::isInstance).count());
            outcomes.stream()
                    .filter(outcome -> !(outcome instanceof ServiceCatalogResult))
                    .forEach(outcome -> assertInstanceOf(ValidationException.class, outcome));
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, catalogJpaRepository.count());
        assertEquals(1, priceJpaRepository.count());
        assertEquals(2, auditLogJpaRepository.count());
    }

    private Object attemptCreate(
            CreateServiceCatalogCommand command,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await(5, TimeUnit.SECONDS);
        try {
            return createService.create(command);
        } catch (RuntimeException exception) {
            return exception;
        }
    }

    private CreateServiceCatalogCommand createCommand(String code, String name, String price) {
        return new CreateServiceCatalogCommand(code, name, new BigDecimal(price), INITIAL_DATE);
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
