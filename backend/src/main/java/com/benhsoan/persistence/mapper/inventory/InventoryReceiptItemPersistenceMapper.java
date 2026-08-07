package com.benhsoan.persistence.mapper.inventory;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.inventory.InventoryReceipt.InventoryReceiptItem;
import com.benhsoan.persistence.entity.inventory.InventoryReceiptItemEntity;

@Component
public class InventoryReceiptItemPersistenceMapper {

    public InventoryReceiptItemEntity toEntity(InventoryReceiptItem domain) {
        if (domain == null) {
            return null;
        }
        return InventoryReceiptItemEntity.builder()
                .id(domain.getId())
                .inventoryReceiptId(domain.getInventoryReceiptId())
                .medicineId(domain.getMedicineId())
                .medicineBatchId(domain.getMedicineBatchId())
                .quantity(domain.getQuantity())
                .importPrice(domain.getImportPrice())
                .totalValue(domain.getTotalValue())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
