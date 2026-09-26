package com.benhsoan.persistence.adapterRepository.inventory;

import java.util.Objects;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.inventory.InventoryReceipt;
import com.benhsoan.persistence.jpaRepository.inventory.JpaInventoryReceiptItemRepository;
import com.benhsoan.persistence.jpaRepository.inventory.JpaInventoryReceiptRepository;
import com.benhsoan.persistence.mapper.inventory.InventoryReceiptItemPersistenceMapper;
import com.benhsoan.persistence.mapper.inventory.InventoryReceiptPersistenceMapper;
import com.benhsoan.port.outbound.repository.inventory.InventoryReceiptRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class InventoryReceiptRepositoryAdapter implements InventoryReceiptRepository {

    private final JpaInventoryReceiptRepository jpaRepository;
    private final JpaInventoryReceiptItemRepository itemJpaRepository;
    private final InventoryReceiptPersistenceMapper mapper;
    private final InventoryReceiptItemPersistenceMapper itemMapper;

    @Override
    public InventoryReceipt save(InventoryReceipt receipt) {
        Objects.requireNonNull(receipt, "Inventory receipt must not be null.");

        jpaRepository.save(mapper.toEntity(receipt));

        itemJpaRepository.saveAll(receipt.getItems()
                .stream()
                .map(itemMapper::toEntity)
                .toList());

        return receipt;
    }
}

