package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.response.inventory.InventoryBatchResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.InventoryExpiryAlertResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.InventoryStockResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.LowStockMedicineResponse;
import com.benhsoan.port.dto.result.InventoryBatchResult;
import com.benhsoan.port.dto.result.InventoryExpiryAlertResult;
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
}
