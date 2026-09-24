package com.benhsoan.persistence.adapterRepository.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.benhsoan.domain.inventory.procurement.MedicationProcurementItem;
import com.benhsoan.domain.inventory.procurement.MedicationProcurementPlan;
import com.benhsoan.domain.inventory.procurement.enums.ProcurementPlanStatus;
import com.benhsoan.persistence.mapper.inventory.MedicationProcurementPersistenceMapper;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({
        MedicationProcurementPlanRepositoryAdapter.class,
        MedicationProcurementPersistenceMapper.class
})
@DisplayName("Medication Procurement Repository Adapter JPA Integration Tests (IT-01)")
class MedicationProcurementPlanRepositoryAdapterIntegrationTest {

    @Autowired
    private MedicationProcurementPlanRepositoryAdapter adapter;

    @Test
    @DisplayName("Lưu mới và cập nhật phiếu dự trù mua thuốc thành công không bị lỗi Action Queue Hibernate")
    void saveAndModifyProcurementPlanSuccess() {
        UUID planId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID medicineId1 = UUID.randomUUID();
        UUID medicineId2 = UUID.randomUUID();
        Instant now = Instant.now();

        MedicationProcurementItem item1 = MedicationProcurementItem.create(
                UUID.randomUUID(),
                planId,
                medicineId1,
                10,
                20,
                30,
                60,
                60,
                "Thuốc thiết yếu",
                now
        );
        MedicationProcurementItem item2 = MedicationProcurementItem.create(
                UUID.randomUUID(),
                planId,
                medicineId2,
                5,
                15,
                20,
                45,
                45,
                "Kháng sinh",
                now
        );

        MedicationProcurementPlan plan = MedicationProcurementPlan.builder()
                .id(planId)
                .planCode("DT000001")
                .status(ProcurementPlanStatus.DRAFT)
                .createdBy(creatorId)
                .periodStartDate(LocalDate.now().minusDays(30))
                .periodEndDate(LocalDate.now())
                .note("Kế hoạch tháng 10")
                .items(List.of(item1, item2))
                .createdAt(now)
                .updatedAt(now)
                .build();

        // 1. Lưu mới
        MedicationProcurementPlan saved = adapter.save(plan);
        assertNotNull(saved);
        assertEquals("DT000001", saved.getPlanCode());
        assertEquals(2, saved.getItems().size());

        // 2. Cập nhật phiếu (thay đổi số lượng và gọi save lần 2 - kiểm tra Action Queue flush)
        plan.submit(now);
        MedicationProcurementPlan updated = adapter.save(plan);
        assertNotNull(updated);
        assertEquals(ProcurementPlanStatus.PENDING_APPROVAL, updated.getStatus());
        assertEquals(2, updated.getItems().size());

        // 3. Tìm kiếm theo ID
        Optional<MedicationProcurementPlan> found = adapter.findById(planId);
        assertTrue(found.isPresent());
        assertEquals("DT000001", found.get().getPlanCode());
        assertEquals(ProcurementPlanStatus.PENDING_APPROVAL, found.get().getStatus());

        // 4. Tìm kiếm theo Plan Code
        Optional<MedicationProcurementPlan> foundByCode = adapter.findByPlanCode("DT000001");
        assertTrue(foundByCode.isPresent());
        assertEquals(planId, foundByCode.get().getId());
    }
}
