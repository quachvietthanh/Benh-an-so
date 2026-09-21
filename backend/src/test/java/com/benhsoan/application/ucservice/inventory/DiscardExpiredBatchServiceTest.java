package com.benhsoan.application.ucservice.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.inventory.StockMovement;
import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.domain.inventory.enums.StockMovementReferenceType;
import com.benhsoan.domain.inventory.enums.StockMovementType;
import com.benhsoan.domain.inventory.exception.BatchAlreadyDiscardedException;
import com.benhsoan.domain.inventory.exception.BatchNotExpiredException;
import com.benhsoan.domain.inventory.exception.BatchNotFoundException;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.inventory.DiscardExpiredBatchCommand;
import com.benhsoan.port.dto.result.DiscardBatchResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;
import com.benhsoan.port.outbound.repository.inventory.StockMovementRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class DiscardExpiredBatchServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-21T08:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();

    private final MedicineBatchRepository medicineBatchRepository = mock(MedicineBatchRepository.class);
    private final MedicineRepository medicineRepository = mock(MedicineRepository.class);
    private final StockMovementRepository stockMovementRepository = mock(StockMovementRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);
    private final LowStockAlertTransitionService lowStockAlertTransitionService = mock(LowStockAlertTransitionService.class);
    private final EligibleStockSnapshotService eligibleStockSnapshotService = mock(EligibleStockSnapshotService.class);
    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);

    private final InventoryManagementAuthorizer authorizer = new InventoryManagementAuthorizer(currentUserPort);

    private DiscardExpiredBatchService service;

    @BeforeEach
    void setUp() {
        service = new DiscardExpiredBatchService(
                medicineBatchRepository,
                medicineRepository,
                stockMovementRepository,
                authorizer,
                eligibleStockSnapshotService,
                lowStockAlertTransitionService,
                auditLogRepository,
                currentUserPort,
                clockPort,
                new com.fasterxml.jackson.databind.ObjectMapper()
        );

        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(USER_ID);
        when(clockPort.now()).thenReturn(NOW);
    }

    @Test
    @DisplayName("discardExpired should mark batch EXPIRED, zero quantity, update catalog and log movement")
    void discardExpiredShouldSucceedForExpiredBatch() {
        UUID batchId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();

        // Expired on 2026-09-10 (before today 2026-09-21)
        MedicineBatch batch = MedicineBatch.restore(
                batchId, medicineId, "BATCH-EXP-001", LocalDate.of(2026, 9, 10),
                50, BatchStatus.ACTIVE, NOW, null
        );
        Medicine medicine = createMedicine(medicineId, "TH001", "Paracetamol 500mg", 50);

        when(medicineBatchRepository.findByIdForUpdate(batchId)).thenReturn(Optional.of(batch));
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));
        when(eligibleStockSnapshotService.snapshotEligibleStockQuantities(any(), any()))
                .thenReturn(Map.of(medicineId, 0));

        DiscardExpiredBatchCommand command = new DiscardExpiredBatchCommand(
                batchId, "Lô hết hạn ngày 10/09/2026, hủy theo biên bản"
        );

        DiscardBatchResult result = service.discardExpired(command);

        assertNotNull(result);
        assertEquals(batchId, result.batchId());
        assertEquals(50, result.discardedQuantity());
        assertEquals(BatchStatus.EXPIRED, result.status());
        assertEquals("Lô hết hạn ngày 10/09/2026, hủy theo biên bản", result.reason());

        verify(medicineBatchRepository).save(batch);
        assertEquals(0, batch.getQuantity());
        assertEquals(BatchStatus.EXPIRED, batch.getStatus());

        verify(medicineRepository).updateStockQuantity(medicineId, -50);
        verify(stockMovementRepository).save(argThat((StockMovement sm) ->
                sm.getMedicineBatchId().equals(batchId)
                        && sm.getMovementType() == StockMovementType.EXPIRE
                        && sm.getReferenceType() == StockMovementReferenceType.EXPIRY_PROCESS
                        && sm.getQuantityChange() == -50
                        && sm.getQuantityBefore() == 50
                        && sm.getQuantityAfter() == 0
                        && sm.getNote().equals("Lô hết hạn ngày 10/09/2026, hủy theo biên bản")
        ));
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("discardExpired should reject when batch is not yet expired")
    void discardExpiredShouldRejectWhenBatchNotYetExpired() {
        UUID batchId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();

        // Expiry date is 2026-10-01 (future date compared to 2026-09-21)
        MedicineBatch batch = MedicineBatch.restore(
                batchId, medicineId, "BATCH-EXP-002", LocalDate.of(2026, 10, 1),
                50, BatchStatus.ACTIVE, NOW, null
        );
        Medicine medicine = createMedicine(medicineId, "TH002", "Ibuprofen 400mg", 50);

        when(medicineBatchRepository.findByIdForUpdate(batchId)).thenReturn(Optional.of(batch));
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));

        DiscardExpiredBatchCommand command = new DiscardExpiredBatchCommand(
                batchId, "Cố ý hủy lô còn hạn"
        );

        assertThrows(BatchNotExpiredException.class, () -> service.discardExpired(command));
        verify(stockMovementRepository, never()).save(any());
        verify(medicineRepository, never()).updateStockQuantity(any(), any(int.class));
    }

    @Test
    @DisplayName("discardExpired should reject when batch is already EXPIRED or quantity is 0")
    void discardExpiredShouldRejectWhenAlreadyExpiredOrZero() {
        UUID batchId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();

        MedicineBatch batch = MedicineBatch.restore(
                batchId, medicineId, "BATCH-EXP-003", LocalDate.of(2026, 9, 10),
                0, BatchStatus.EXPIRED, NOW, null
        );
        Medicine medicine = createMedicine(medicineId, "TH003", "Amoxicillin 500mg", 0);

        when(medicineBatchRepository.findByIdForUpdate(batchId)).thenReturn(Optional.of(batch));
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));

        DiscardExpiredBatchCommand command = new DiscardExpiredBatchCommand(
                batchId, "Hủy lần hai"
        );

        assertThrows(BatchAlreadyDiscardedException.class, () -> service.discardExpired(command));
    }

    @Test
    @DisplayName("discardExpired should reject when reason is null or blank (QTN-32)")
    void discardExpiredShouldRejectWhenReasonIsBlank() {
        UUID batchId = UUID.randomUUID();

        assertThrows(ValidationException.class, () ->
                service.discardExpired(new DiscardExpiredBatchCommand(batchId, null)));
        assertThrows(ValidationException.class, () ->
                service.discardExpired(new DiscardExpiredBatchCommand(batchId, "   ")));
    }

    @Test
    @DisplayName("discardExpired should throw BatchNotFoundException when batch does not exist")
    void discardExpiredShouldThrowBatchNotFound() {
        UUID batchId = UUID.randomUUID();
        when(medicineBatchRepository.findByIdForUpdate(batchId)).thenReturn(Optional.empty());

        assertThrows(BatchNotFoundException.class, () ->
                service.discardExpired(new DiscardExpiredBatchCommand(batchId, "Lý do hợp lệ")));
    }

    @Test
    @DisplayName("discardExpired should throw AccessDeniedException when user is not pharmacist or admin")
    void discardExpiredShouldRejectUnauthorizedUser() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(false);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        UUID batchId = UUID.randomUUID();
        assertThrows(AccessDeniedException.class, () ->
                service.discardExpired(new DiscardExpiredBatchCommand(batchId, "Lý do hợp lệ")));
    }

    @Test
    @DisplayName("discardExpired should throw AccessDeniedException when user only has PHARMACY_READ permission")
    void discardExpiredShouldRejectUserWithOnlyPharmacyRead() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(false);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasPermission("PHARMACY_UPDATE")).thenReturn(false);
        when(currentUserPort.hasPermission("PHARMACY_READ")).thenReturn(true);

        UUID batchId = UUID.randomUUID();
        assertThrows(AccessDeniedException.class, () ->
                service.discardExpired(new DiscardExpiredBatchCommand(batchId, "Lý do hợp lệ")));
    }

    @Test
    @DisplayName("discardExpired should safely serialize reason with quotes and special characters in audit log detail")
    void discardExpiredShouldSafelySerializeSpecialCharactersInReason() {
        UUID batchId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();

        MedicineBatch batch = MedicineBatch.restore(
                batchId, medicineId, "BATCH-EXP-001", LocalDate.of(2026, 9, 10),
                50, BatchStatus.ACTIVE, NOW, null
        );
        Medicine medicine = createMedicine(medicineId, "TH001", "Paracetamol 500mg", 50);

        when(medicineBatchRepository.findByIdForUpdate(batchId)).thenReturn(Optional.of(batch));
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));
        when(eligibleStockSnapshotService.snapshotEligibleStockQuantities(any(), any()))
                .thenReturn(Map.of(medicineId, 0));

        String specialReason = "Hủy lô \"ẩm mốc\" \\ nứt vỡ 50 lọ\nĐã có biên bản";
        service.discardExpired(new DiscardExpiredBatchCommand(batchId, specialReason));

        verify(auditLogRepository).save(argThat((AuditLog log) -> {
            try {
                com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(log.getDetail());
                return specialReason.equals(node.get("reason").asText())
                        && node.get("discardedQuantity").asInt() == 50;
            } catch (Exception e) {
                return false;
            }
        }));
    }

    private Medicine createMedicine(UUID id, String code, String name, int stock) {
        return Medicine.restore(
                id, code, name, "Hoạt chất", "500mg",
                DosageForm.TABLET, "Viên", AdministrationRoute.ORAL,
                true, NOW, null, stock, 10
        );
    }
}
