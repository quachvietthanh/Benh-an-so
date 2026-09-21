package com.benhsoan.persistence.jpaRepository.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.billing.enums.DiscountRequestStatus;
import com.benhsoan.domain.billing.enums.DiscountType;
import com.benhsoan.persistence.entity.billing.DiscountRequestEntity;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class DiscountRequestRepositoryJpaIntegrationTest {

    @Autowired
    private JpaDiscountRequestRepository repository;

    @Test
    void savesAndFindsPendingDiscountRequestByVisitIdAndStatus() {
        UUID visitId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-21T10:00:00Z");

        DiscountRequestEntity entity = DiscountRequestEntity.builder()
                .id(requestId)
                .visitId(visitId)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20.00"))
                .originalAmount(new BigDecimal("500000.00"))
                .discountAmount(new BigDecimal("100000.00"))
                .finalAmount(new BigDecimal("400000.00"))
                .reason("Bệnh nhân có hoàn cảnh khó khăn")
                .status(DiscountRequestStatus.PENDING)
                .requestedBy(requesterId)
                .requestedAt(now)
                .build();

        repository.saveAndFlush(entity);

        assertTrue(repository.existsByVisitIdAndStatus(visitId, DiscountRequestStatus.PENDING));
        assertFalse(repository.existsByVisitIdAndStatus(visitId, DiscountRequestStatus.APPROVED));

        var found = repository.findByVisitIdAndStatus(visitId, DiscountRequestStatus.PENDING);
        assertTrue(found.isPresent());
        assertEquals(requestId, found.get().getId());
        assertEquals(new BigDecimal("100000.00"), found.get().getDiscountAmount());
        assertEquals(DiscountType.PERCENTAGE, found.get().getDiscountType());
    }

    @Test
    void searchesDiscountRequestsWithFilters() {
        UUID visitId1 = UUID.randomUUID();
        UUID visitId2 = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID approverId = UUID.randomUUID();
        Instant t1 = Instant.parse("2026-09-21T08:00:00Z");
        Instant t2 = Instant.parse("2026-09-21T09:00:00Z");

        repository.saveAndFlush(DiscountRequestEntity.builder()
                .id(UUID.randomUUID())
                .visitId(visitId1)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .originalAmount(new BigDecimal("200000.00"))
                .discountAmount(new BigDecimal("20000.00"))
                .finalAmount(new BigDecimal("180000.00"))
                .reason("Lý do 1")
                .status(DiscountRequestStatus.PENDING)
                .requestedBy(requesterId)
                .requestedAt(t1)
                .build());

        repository.saveAndFlush(DiscountRequestEntity.builder()
                .id(UUID.randomUUID())
                .visitId(visitId2)
                .discountType(DiscountType.FULL_FREE)
                .discountValue(new BigDecimal("100.00"))
                .originalAmount(new BigDecimal("300000.00"))
                .discountAmount(new BigDecimal("300000.00"))
                .finalAmount(BigDecimal.ZERO)
                .reason("Miễn phí 100%")
                .status(DiscountRequestStatus.APPROVED)
                .requestedBy(requesterId)
                .requestedAt(t2)
                .approvedBy(approverId)
                .approvedAt(t2.plusSeconds(300))
                .build());

        Page<DiscountRequestEntity> pendingResults = repository.search(
                null,
                DiscountRequestStatus.PENDING,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );
        assertEquals(1, pendingResults.getTotalElements());
        assertEquals(DiscountRequestStatus.PENDING, pendingResults.getContent().get(0).getStatus());

        Page<DiscountRequestEntity> freeResults = repository.search(
                null,
                null,
                DiscountType.FULL_FREE,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );
        assertEquals(1, freeResults.getTotalElements());
        assertEquals(DiscountType.FULL_FREE, freeResults.getContent().get(0).getDiscountType());

        Page<DiscountRequestEntity> byVisit = repository.search(
                visitId1,
                null,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );
        assertEquals(1, byVisit.getTotalElements());
        assertEquals(visitId1, byVisit.getContent().get(0).getVisitId());
    }
}
