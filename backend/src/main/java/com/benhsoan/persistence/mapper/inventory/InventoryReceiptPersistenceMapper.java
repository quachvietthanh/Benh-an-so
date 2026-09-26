package com.benhsoan.persistence.mapper.inventory;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.inventory.InventoryReceipt;
import com.benhsoan.persistence.entity.inventory.InventoryReceiptEntity;

@Component
public class InventoryReceiptPersistenceMapper {

    public InventoryReceiptEntity toEntity(InventoryReceipt domain) {
        if (domain == null) {
            return null;
        }
        return InventoryReceiptEntity.builder()
                .id(domain.getId())
                .receivedBy(domain.getReceivedBy())
                .receivedAt(domain.getReceivedAt())
                .note(domain.getNote())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
