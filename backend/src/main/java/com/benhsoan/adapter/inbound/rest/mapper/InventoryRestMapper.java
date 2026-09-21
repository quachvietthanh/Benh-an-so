package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.inventory.AdjustBatchStockRequest;
import com.benhsoan.adapter.inbound.rest.request.inventory.DiscardExpiredBatchRequest;
import com.benhsoan.adapter.inbound.rest.response.inventory.BatchAdjustmentResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.DiscardBatchResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.InventoryBatchResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.InventoryExpiryAlertResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.InventoryStockReportItemResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.InventoryStockReportResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.InventoryStockResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.LowStockMedicineResponse;
import com.benhsoan.port.dto.command.inventory.AdjustBatchStockCommand;
import com.benhsoan.port.dto.command.inventory.DiscardExpiredBatchCommand;
import com.benhsoan.port.dto.result.BatchAdjustmentResult;
import com.benhsoan.port.dto.result.DiscardBatchResult;
import com.benhsoan.port.dto.result.InventoryBatchResult;
import com.benhsoan.port.dto.result.InventoryExpiryAlertResult;
import com.benhsoan.port.dto.result.InventoryStockReportItemResult;
import com.benhsoan.port.dto.result.InventoryStockReportResult;
import com.benhsoan.port.dto.result.InventoryStockResult;
import com.benhsoan.port.dto.result.LowStockMedicineResult;

@Component
public class InventoryRestMapper {

    public List<InventoryBatchResponse> toBatchResponses(List<InventoryBatchResult> results) {
        return results.stream()
                .map(this::toBatchResponse)
                .toList();
    }

    public List<InventoryStockResponse> toStockResponses(List<InventoryStockResult> results) {
        return results.stream()
                .map(this::toStockResponse)
                .toList();
    }

    public List<LowStockMedicineResponse> toLowStockResponses(List<LowStockMedicineResult> results) {
        return results.stream()
                .map(this::toLowStockResponse)
                .toList();
    }

    public List<InventoryExpiryAlertResponse> toExpiryAlertResponses(List<InventoryExpiryAlertResult> results) {
        return results.stream()
                .map(this::toExpiryAlertResponse)
                .toList();
    }

    public InventoryStockReportResponse toStockReportResponse(InventoryStockReportResult result) {
        return new InventoryStockReportResponse(
                result.from(),
                result.to(),
                result.generatedAt(),
                result.hasTransactions(),
                result.items().stream().map(this::toStockReportItemResponse).toList()
        );
    }

    private InventoryStockReportItemResponse toStockReportItemResponse(InventoryStockReportItemResult result) {
        return new InventoryStockReportItemResponse(
                result.medicineId(),
                result.medicineCode(),
                result.medicineName(),
                result.unit(),
                result.openingQuantity(),
                result.receivedQuantity(),
                result.dispensedQuantity(),
                result.returnedQuantity(),
                result.adjustedQuantity(),
                result.closingQuantity()
        );
    }

    private InventoryBatchResponse toBatchResponse(InventoryBatchResult result) {
        return new InventoryBatchResponse(
                result.batchId(),
                result.medicineId(),
                result.medicineCode(),
                result.medicineName(),
                result.batchNumber(),
                result.expiryDate(),
                result.quantity(),
                result.status(),
                result.eligibleForDispense(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    private InventoryStockResponse toStockResponse(InventoryStockResult result) {
        return new InventoryStockResponse(
                result.medicineId(),
                result.medicineCode(),
                result.medicineName(),
                result.activeIngredient(),
                result.strength(),
                result.unit(),
                result.active(),
                result.stockQuantity(),
                result.eligibleStockQuantity(),
                result.activeBatchCount(),
                result.nearestExpiryDate()
        );
    }

    private LowStockMedicineResponse toLowStockResponse(LowStockMedicineResult result) {
        return new LowStockMedicineResponse(
                result.medicineId(),
                result.medicineCode(),
                result.medicineName(),
                result.unit(),
                result.stockQuantity(),
                result.eligibleStockQuantity(),
                result.minStockThreshold(),
                result.shortageQuantity()
        );
    }

    private InventoryExpiryAlertResponse toExpiryAlertResponse(InventoryExpiryAlertResult result) {
        return new InventoryExpiryAlertResponse(
                result.batchId(),
                result.medicineId(),
                result.medicineCode(),
                result.medicineName(),
                result.batchNumber(),
                result.expiryDate(),
                result.quantity(),
                result.batchStatus(),
                result.daysToExpiry(),
                result.alertStatus(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    public AdjustBatchStockCommand toAdjustCommand(UUID batchId, AdjustBatchStockRequest request) {
        return new AdjustBatchStockCommand(batchId, request.actualQuantity(), request.reason());
    }

    public BatchAdjustmentResponse toAdjustResponse(BatchAdjustmentResult result) {
        return new BatchAdjustmentResponse(
                result.batchId(),
                result.medicineId(),
                result.medicineCode(),
                result.medicineName(),
                result.batchNumber(),
                result.expiryDate(),
                result.quantityBefore(),
                result.quantityAfter(),
                result.quantityChange(),
                result.status(),
                result.reason(),
                result.performedBy(),
                result.performedAt()
        );
    }

    public DiscardExpiredBatchCommand toDiscardCommand(UUID batchId, DiscardExpiredBatchRequest request) {
        return new DiscardExpiredBatchCommand(batchId, request.reason());
    }

    public DiscardBatchResponse toDiscardResponse(DiscardBatchResult result) {
        return new DiscardBatchResponse(
                result.batchId(),
                result.medicineId(),
                result.medicineCode(),
                result.medicineName(),
                result.batchNumber(),
                result.expiryDate(),
                result.discardedQuantity(),
                result.status(),
                result.reason(),
                result.performedBy(),
                result.performedAt()
        );
    }
}
