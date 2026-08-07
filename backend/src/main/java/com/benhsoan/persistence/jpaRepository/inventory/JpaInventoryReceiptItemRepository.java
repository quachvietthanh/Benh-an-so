package com.benhsoan.persistence.jpaRepository.inventory;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.inventory.InventoryReceiptItemEntity;

public interface JpaInventoryReceiptItemRepository
        extends JpaRepository<InventoryReceiptItemEntity, UUID> {
}
