package com.benhsoan.application.ucservice.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
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
import com.benhsoan.domain.inventory.exception.BatchNotFoundException;
import com.benhsoan.domain.inventory.exception.BatchStateConflictException;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.inventory.AdjustBatchStockCommand;
import com.benhsoan.port.dto.result.BatchAdjustmentResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;
import com.benhsoan.port.outbound.repository.inventory.StockMovementRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class AdjustBatchStockServiceTest {

        private static final Instant NOW = Instant.parse("2026-09-21T08:00:00Z");
        private static final UUID USER_ID = UUID.randomUUID();

        private final MedicineBatchRepository medicineBatchRepository = mock(MedicineBatchRepository.class);
        private final MedicineRepository medicineRepository = mock(MedicineRepository.class);
        private final StockMovementRepository stockMovementRepository = mock(StockMovementRepository.class);
        private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        private final ClockPort clockPort = mock(ClockPort.class);
        private final LowStockAlertTransitionService lowStockAlertTransitionService = mock(
                        LowStockAlertTransitionService.class);
        private final EligibleStockSnapshotService eligibleStockSnapshotService = mock(
                        EligibleStockSnapshotService.class);
        private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);

        private final InventoryManagementAuthorizer authorizer = new InventoryManagementAuthorizer(currentUserPort);

        private AdjustBatchStockService service;

        @BeforeEach
        void setUp() {
                service = new AdjustBatchStockService(
                                medicineBatchRepository,
                                medicineRepository,
                                stockMovementRepository,
                                authorizer,
                                eligibleStockSnapshotService,
                                lowStockAlertTransitionService,
                                auditLogRepository,
                                currentUserPort,
                                clockPort,
                                new com.fasterxml.jackson.databind.ObjectMapper());

                when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);
                when(currentUserPort.getCurrentUserId()).thenReturn(USER_ID);
                when(clockPort.now()).thenReturn(NOW);
        }

        @Test
        @DisplayName("adjustStock should reduce stock, save StockMovement and update catalog when actual quantity is less")
        void adjustStockShouldReduceStockSuccessfully() {
                UUID batchId = UUID.randomUUID();
                UUID medicineId = UUID.randomUUID();

                MedicineBatch batch = MedicineBatch.restore(
                                batchId, medicineId, "BATCH-001", LocalDate.of(2027, 12, 31),
                                100, BatchStatus.ACTIVE, NOW, null);
                Medicine medicine = createMedicine(medicineId, "TH001", "Paracetamol 500mg", 100);

                when(medicineBatchRepository.findByIdForUpdate(batchId)).thenReturn(Optional.of(batch));
                when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));
                when(eligibleStockSnapshotService.snapshotEligibleStockQuantities(any(), any()))
                                .thenReturn(Map.of(medicineId, 100));

                AdjustBatchStockCommand command = new AdjustBatchStockCommand(
                                batchId, 85, "Kiểm kê thiếu 15 viên");

                BatchAdjustmentResult result = service.adjustStock(command);

                assertNotNull(result);
                assertEquals(85, result.quantityAfter());
                assertEquals(100, result.quantityBefore());
                assertEquals(-15, result.quantityChange());
                assertEquals(BatchStatus.ACTIVE, result.status());
                assertEquals("Kiểm kê thiếu 15 viên", result.reason());

                verify(medicineBatchRepository).save(batch);
                verify(medicineRepository).updateStockQuantity(medicineId, -15);
                verify(stockMovementRepository)
                                .save(argThat((StockMovement sm) -> sm.getMedicineBatchId().equals(batchId)
                                                && sm.getMovementType() == StockMovementType.ADJUSTMENT
                                                && sm.getReferenceType() == StockMovementReferenceType.MANUAL_ADJUSTMENT
                                                && sm.getQuantityChange() == -15
                                                && sm.getQuantityBefore() == 100
                                                && sm.getQuantityAfter() == 85
                                                && sm.getNote().equals("Kiểm kê thiếu 15 viên")));
                verify(auditLogRepository).save(any(AuditLog.class));
        }

        @Test
        @DisplayName("adjustStock should increase stock and update catalog when actual quantity is more")
        void adjustStockShouldIncreaseStockSuccessfully() {
                UUID batchId = UUID.randomUUID();
                UUID medicineId = UUID.randomUUID();

                MedicineBatch batch = MedicineBatch.restore(
                                batchId, medicineId, "BATCH-002", LocalDate.of(2027, 12, 31),
                                50, BatchStatus.ACTIVE, NOW, null);
                Medicine medicine = createMedicine(medicineId, "TH002", "Ibuprofen 400mg", 50);

                when(medicineBatchRepository.findByIdForUpdate(batchId)).thenReturn(Optional.of(batch));
                when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));
                when(eligibleStockSnapshotService.snapshotEligibleStockQuantities(any(), any()))
                                .thenReturn(Map.of(medicineId, 50));

                AdjustBatchStockCommand command = new AdjustBatchStockCommand(
                                batchId, 60, "Kiểm kê thừa 10 viên");

                BatchAdjustmentResult result = service.adjustStock(command);

                assertEquals(60, result.quantityAfter());
                assertEquals(10, result.quantityChange());

                verify(medicineRepository).updateStockQuantity(medicineId, 10);
        }

        @Test
        @DisplayName("adjustStock should mark batch DEPLETED when actual quantity is 0")
        void adjustStockShouldMarkBatchDepleted() {
                UUID batchId = UUID.randomUUID();
                UUID medicineId = UUID.randomUUID();

                MedicineBatch batch = MedicineBatch.restore(
                                batchId, medicineId, "BATCH-003", LocalDate.of(2027, 12, 31),
                                30, BatchStatus.ACTIVE, NOW, null);
                Medicine medicine = createMedicine(medicineId, "TH003", "Amoxicillin 500mg", 30);

                when(medicineBatchRepository.findByIdForUpdate(batchId)).thenReturn(Optional.of(batch));
                when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));

                AdjustBatchStockCommand command = new AdjustBatchStockCommand(
                                batchId, 0, "Hỏng toàn bộ do ngấm nước");

                BatchAdjustmentResult result = service.adjustStock(command);

                assertEquals(0, result.quantityAfter());
                assertEquals(BatchStatus.DEPLETED, result.status());
                verify(medicineRepository).updateStockQuantity(medicineId, -30);
        }

        @Test
        @DisplayName("adjustStock should reject when actual quantity equals current batch stock (difference is zero)")
        void adjustStockShouldRejectWhenDifferenceIsZero() {
                UUID batchId = UUID.randomUUID();
                UUID medicineId = UUID.randomUUID();

                MedicineBatch batch = MedicineBatch.restore(
                                batchId, medicineId, "BATCH-004", LocalDate.of(2027, 12, 31),
                                100, BatchStatus.ACTIVE, NOW, null);

                when(medicineBatchRepository.findByIdForUpdate(batchId)).thenReturn(Optional.of(batch));

                AdjustBatchStockCommand command = new AdjustBatchStockCommand(
                                batchId, 100, "Không có chênh lệch");

                ValidationException ex = assertThrows(ValidationException.class, () -> service.adjustStock(command));
                assertEquals("Số lượng kiểm kê thực tế trùng khớp với tồn kho hiện tại, không có chênh lệch để điều chỉnh.",
                                ex.getMessage());

                verify(medicineRepository, never()).updateStockQuantity(any(), anyInt());
                verify(stockMovementRepository, never()).save(any());
        }

        @Test
        @DisplayName("adjustStock should reject when reason is null or blank (QTN-32)")
        void adjustStockShouldRejectWhenReasonIsBlank() {
                UUID batchId = UUID.randomUUID();

                assertThrows(ValidationException.class,
                                () -> service.adjustStock(new AdjustBatchStockCommand(batchId, 50, null)));
                assertThrows(ValidationException.class,
                                () -> service.adjustStock(new AdjustBatchStockCommand(batchId, 50, "   ")));
        }

        @Test
        @DisplayName("adjustStock should reject negative actual quantity")
        void adjustStockShouldRejectNegativeQuantity() {
                UUID batchId = UUID.randomUUID();

                assertThrows(ValidationException.class,
                                () -> service.adjustStock(new AdjustBatchStockCommand(batchId, -5, "Lý do hợp lệ")));
        }

        @Test
        @DisplayName("adjustStock should throw BatchNotFoundException when batch does not exist")
        void adjustStockShouldThrowBatchNotFound() {
                UUID batchId = UUID.randomUUID();
                when(medicineBatchRepository.findByIdForUpdate(batchId)).thenReturn(Optional.empty());

                assertThrows(BatchNotFoundException.class,
                                () -> service.adjustStock(new AdjustBatchStockCommand(batchId, 50, "Lý do hợp lệ")));
        }

        @Test
        @DisplayName("adjustStock should throw BatchStateConflictException when batch is already EXPIRED")
        void adjustStockShouldRejectExpiredBatch() {
                UUID batchId = UUID.randomUUID();
                UUID medicineId = UUID.randomUUID();

                MedicineBatch batch = MedicineBatch.restore(
                                batchId, medicineId, "BATCH-005", LocalDate.of(2026, 8, 1),
                                0, BatchStatus.EXPIRED, NOW, null);
                Medicine medicine = createMedicine(medicineId, "TH001", "Paracetamol 500mg", 100);

                when(medicineBatchRepository.findByIdForUpdate(batchId)).thenReturn(Optional.of(batch));
                when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));

                assertThrows(BatchStateConflictException.class,
                                () -> service.adjustStock(new AdjustBatchStockCommand(batchId, 10, "Lý do hợp lệ")));
        }

        @Test
        @DisplayName("adjustStock should throw AccessDeniedException when user is not pharmacist or admin")
        void adjustStockShouldRejectUnauthorizedUser() {
                when(currentUserPort.hasRole("PHARMACIST")).thenReturn(false);
                when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

                UUID batchId = UUID.randomUUID();
                assertThrows(AccessDeniedException.class,
                                () -> service.adjustStock(new AdjustBatchStockCommand(batchId, 10, "Lý do hợp lệ")));
        }

        @Test
        @DisplayName("adjustStock should throw AccessDeniedException when user only has PHARMACY_READ permission")
        void adjustStockShouldRejectUserWithOnlyPharmacyRead() {
                when(currentUserPort.hasRole("PHARMACIST")).thenReturn(false);
                when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
                when(currentUserPort.hasPermission("PHARMACY_UPDATE")).thenReturn(false);
                when(currentUserPort.hasPermission("PHARMACY_READ")).thenReturn(true);

                UUID batchId = UUID.randomUUID();
                assertThrows(AccessDeniedException.class,
                                () -> service.adjustStock(new AdjustBatchStockCommand(batchId, 10, "Lý do hợp lệ")));
        }

        @Test
        @DisplayName("adjustStock should safely serialize reason with quotes and special characters in audit log detail")
        void adjustStockShouldSafelySerializeSpecialCharactersInReason() {
                UUID batchId = UUID.randomUUID();
                UUID medicineId = UUID.randomUUID();

                MedicineBatch batch = MedicineBatch.restore(
                                batchId, medicineId, "BATCH-001", LocalDate.of(2027, 12, 31),
                                100, BatchStatus.ACTIVE, NOW, null);
                Medicine medicine = createMedicine(medicineId, "TH001", "Paracetamol 500mg", 100);

                when(medicineBatchRepository.findByIdForUpdate(batchId)).thenReturn(Optional.of(batch));
                when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));
                when(eligibleStockSnapshotService.snapshotEligibleStockQuantities(any(), any()))
                                .thenReturn(Map.of(medicineId, 100));

                String specialReason = "Kiểm kê \"Khu A\" \\ vỡ 15 lọ\nĐã lập biên bản";
                service.adjustStock(new AdjustBatchStockCommand(batchId, 85, specialReason));

                verify(auditLogRepository).save(argThat((AuditLog log) -> {
                        try {
                                com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper()
                                                .readTree(log.getDetail());
                                return specialReason.equals(node.get("reason").asText())
                                                && node.get("quantityChange").asInt() == -15;
                        } catch (Exception e) {
                                return false;
                        }
                }));
        }

        @Test
        @DisplayName("adjustStock should reject adjustment when batch expiry date is before today")
        void adjustStockShouldRejectWhenBatchIsExpiredByDate() {
                UUID batchId = UUID.randomUUID();
                UUID medicineId = UUID.randomUUID();

                MedicineBatch expiredBatch = MedicineBatch.restore(
                                batchId, medicineId, "BATCH-EXPIRED", LocalDate.of(2026, 9, 1),
                                100, BatchStatus.ACTIVE, NOW, null);
                Medicine medicine = createMedicine(medicineId, "TH001", "Paracetamol 500mg", 100);

                when(medicineBatchRepository.findByIdForUpdate(batchId)).thenReturn(Optional.of(expiredBatch));
                when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));

                assertThrows(BatchStateConflictException.class,
                                () -> service.adjustStock(new AdjustBatchStockCommand(batchId, 85, "Kiểm kê")));
        }

        private Medicine createMedicine(UUID id, String code, String name, int stock) {
                return Medicine.restore(
                                id, code, name, "Hoạt chất", "500mg",
                                DosageForm.TABLET, "Viên", AdministrationRoute.ORAL,
                                true, NOW, null, stock, 10);
        }
}
