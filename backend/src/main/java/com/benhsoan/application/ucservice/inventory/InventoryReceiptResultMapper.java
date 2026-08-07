package com.benhsoan.application.ucservice.inventory;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.inventory.InventoryReceipt;
import com.benhsoan.domain.inventory.InventoryReceiptItem;
import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.port.dto.result.InventoryReceiptItemResult;
import com.benhsoan.port.dto.result.InventoryReceiptResult;

@Component
class InventoryReceiptResultMapper {

    InventoryReceiptResult toResult(
            InventoryReceipt receipt,
            List<MedicineBatch> batches
    ) {
        List<InventoryReceiptItemResult> itemResults = receipt.getItems()
                .stream()
                .map(item -> toItemResult(item, batches))
                .toList();

        return new InventoryReceiptResult(
                receipt.getId(),
                receipt.getReceivedBy(),
                receipt.getReceivedAt(),
                receipt.getNote(),
                receipt.getCreatedAt(),
                itemResults
        );
    }

    private InventoryReceiptItemResult toItemResult(
            InventoryReceiptItem item,
            List<MedicineBatch> batches
    ) {
        MedicineBatch batch = batches.stream()
                .filter(b -> b.getId().equals(item.getMedicineBatchId()))
                .findFirst()
                .orElseThrow();

        return new InventoryReceiptItemResult(
                item.getId(),
                item.getMedicineId(),
                batch.getBatchNumber(),
                batch.getExpiryDate(),
                item.getQuantity(),
                item.getImportPrice(),
                item.getTotalValue()
        );
    }
}
